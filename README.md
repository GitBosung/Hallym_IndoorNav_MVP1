# Hallym Indoor Navigation MVP

## 개요
이 프로젝트는 Android 환경에서 Indoor Navigation 시스템의 MVP(Minimum Viable Product)를 구현한 것입니다.  
앱은 1초 동안 50개의 센서 데이터를 수집(Accelerometer, Gyroscope, Game Rotation Vector)하여, 딥러닝(TensorFlow Lite) 모델을 통해 1초 동안의 속도와 헤딩(방향) 변화를 추정합니다. 추정된 결과는 지도 화면에 실시간으로 시각화됩니다.

## 특징
- **센서 데이터 수집**  
  - 1초 동안 50개의 센서 데이터를 수집하여 모델 입력으로 사용합니다.
  - 각 센서 데이터는 가속도, 자이로, 게임 회전 벡터의 3축 값을 포함해 총 9개의 특성으로 구성됩니다.
  
- **딥러닝 기반 추정**  
  - TensorFlow Lite 모델은 입력 텐서의 형태를 `[1, 50, 9]` (배치 크기 1, 시퀀스 길이 50, 특성 차원 9)로 받아 1초 동안의 속도와 헤딩 변화량을 `[1, 2]` 형태로 출력합니다.
  - Flex delegate를 통해 TensorFlow Lite의 선택적 연산(Flex Ops)을 지원합니다.
  
- **실시간 시각화**  
  - Jetpack Compose를 사용해 지도와 이동 경로를 Canvas 위에 실시간으로 그려줍니다.
  - 현재 위치는 파란 점, 이동 경로는 빨간 선으로 표시됩니다.

## 설치 및 실행
1. **프로젝트 클론**
   ```bash
   git clone https://github.com/yourusername/Hallym_indoor_nav_MVP.git
   cd Hallym_indoor_nav_MVP
