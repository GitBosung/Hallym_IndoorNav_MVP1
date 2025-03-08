package com.example.hallym_indoor_nav_mvp

import android.content.res.AssetFileDescriptor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.cos
import kotlin.math.sin

class MapActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var gameRotationVector: Sensor? = null

    private lateinit var lstmModel: Interpreter
    private val sensorDataList = mutableListOf<SensorData>()

    private var lastAccelData = FloatArray(3) { 0f }
    private var lastGyroData = FloatArray(3) { 0f }
    private var lastGameRotationData = FloatArray(3) { 0f }

    private var xPos by mutableStateOf(500f)
    private var yPos by mutableStateOf(500f)
    private val path = mutableListOf<Offset>()

    private var headingAngle = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MapActivity", "MapActivity 시작됨")

        // TFLite 모델 로드
        lstmModel = Interpreter(loadModelFile("pdr_model.tflite"))

        // 센서 초기화
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gameRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                MapScreen(Modifier.padding(innerPadding), xPos, yPos, path)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gameRotationVector?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        Log.d("MapActivity", "센서 해제됨")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> lastAccelData = it.values.copyOf()
                Sensor.TYPE_GYROSCOPE -> lastGyroData = it.values.copyOf()
                Sensor.TYPE_GAME_ROTATION_VECTOR -> lastGameRotationData = it.values.copyOf()
            }

            val timestamp = System.currentTimeMillis()
            sensorDataList.add(
                SensorData(
                    timestamp,
                    lastAccelData[0], lastAccelData[1], lastAccelData[2],
                    lastGyroData[0], lastGyroData[1], lastGyroData[2],
                    lastGameRotationData[0], lastGameRotationData[1], lastGameRotationData[2]
                )
            )

            // 50개 데이터 누적 시 모델 추론
            if (sensorDataList.size >= 50) {
                processPDR()
                sensorDataList.clear()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 정확도 변경은 여기서는 처리하지 않음
    }

    /**
     * PDR 추론 로직
     */
    private fun processPDR() {
        if (sensorDataList.size >= 50) {
            // (1, 50, 9) 형태의 3차원 배열 생성
            val inputArray = Array(1) { Array(50) { FloatArray(9) } }
            for (i in 0 until 50) {
                val data = sensorDataList[i]
                inputArray[0][i][0] = data.accelX
                inputArray[0][i][1] = data.accelY
                inputArray[0][i][2] = data.accelZ
                inputArray[0][i][3] = data.gyroX
                inputArray[0][i][4] = data.gyroY
                inputArray[0][i][5] = data.gyroZ
                inputArray[0][i][6] = data.gameRotX
                inputArray[0][i][7] = data.gameRotY
                inputArray[0][i][8] = data.gameRotZ
            }

            // 출력 텐서의 형태 [1, 2]에 맞게 출력 배열을 2차원으로 선언
            val output = Array(1) { FloatArray(2) }
            lstmModel.run(inputArray, output)

            // 출력 결과에 맞게 값 전달
            applyPDR(output[0][0], output[0][1])
        }
    }


    /**
     * 추론 결과를 바탕으로 위치 갱신
     */
    private fun applyPDR(predictedSpeed: Float, predictedHeadingChange: Float) {
        // 헤딩(heading) 변경
        headingAngle += predictedHeadingChange

        // speed와 headingAngle을 이용해 이동 거리 계산
        val deltaX = predictedSpeed * cos(predictedHeadingChange)
        val deltaY = predictedSpeed * sin(predictedHeadingChange)

        // 실제 좌표에 반영 (가시화를 위해 100 배율)
        xPos += deltaX * 50
        yPos += deltaY * 50
        path.add(Offset(xPos, yPos))

        Log.d("PDR", "이동 발생 - X: $xPos, Y: $yPos, 속도: $predictedSpeed, 헤딩 변화: $predictedHeadingChange")
    }

    /**
     * TFLite 모델 로드 함수
     */
    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    xPos: Float,
    yPos: Float,
    path: List<Offset>
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("건물 도면 화면", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 화면 중앙을 기준점(0,0)처럼 사용
            val centerOffset = Offset(size.width / 2, size.height / 2)
            // 경로 점들을 현재 위치를 기준으로 재계산
            val adjustedPath = path.map {
                it - Offset(xPos - centerOffset.x, yPos - centerOffset.y)
            }

            // 현재 위치 (파란 점)
            drawCircle(
                color = Color.Blue,
                radius = 10f,
                center = centerOffset
            )

            // 이동 경로(빨간 선)
            for (i in 1 until adjustedPath.size) {
                drawLine(
                    color = Color.Red,
                    start = adjustedPath[i - 1],
                    end = adjustedPath[i],
                    strokeWidth = 5f
                )
            }
        }
    }
}

/**
 * 센서 데이터를 담는 데이터 클래스
 */
data class SensorData(
    val timestamp: Long,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val gameRotX: Float,
    val gameRotY: Float,
    val gameRotZ: Float
)
