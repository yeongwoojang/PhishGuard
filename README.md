# 🛡️ PhishGuard

> AI 기반 피싱 문자 실시간 탐지 Android 앱

[![Android](https://img.shields.io/badge/Android-26%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## 📱 소개

PhishGuard는 수신되는 문자를 실시간으로 분석하여 피싱·스미싱 문자를 자동으로 탐지하는 Android 앱입니다.

**온디바이스 TFLite + Gemini API** 의 2단계 분석으로
정확도를 높이면서 개인정보 노출을 최소화했습니다.

---

## ✨ 주요 기능

- 🔍 **실시간 문자 감지** — 문자 수신 즉시 백그라운드에서 자동 분석
- 🤖 **2단계 AI 분석** — 온디바이스 TFLite → Gemini API 순차 분석
- 📝 **긴 문자 청크 분석** — 30단어 초과 문자도 청크로 나눠 놓치지 않고 분석
- 💬 **판별 이유 설명** — "왜 위험한지" Gemini가 자연어로 설명
- 📋 **탐지 이력 관리** — 위험/주의/안전 이력 자동 저장
- 🔐 **생체 인증** — 앱 진입 시 지문/얼굴 인식으로 잠금

---

## 🔒 보안 설계

- **개인정보 최소화**
  - TFLite + 규칙 기반으로 1차 판별 → 대부분 기기 내에서만 처리
  - 판단이 애매한 케이스(점수 0.31~0.60)만 Gemini API로 전송
  - 전체 문자의 대부분은 외부 서버로 나가지 않음
  - TFLite 모델 학습: 피싱문자 102개 + 정상문자 62개 = 164개 x 50(데이터 증강을 위해 50을 곱함) → 8,200개

---

## 🎥 시연

| 피싱 문자 탐지 | 경고 알림 | 탐지 이력 |
|:---:|:---:|:---:|
| 준비 중 | 준비 중 | 준비 중 |
