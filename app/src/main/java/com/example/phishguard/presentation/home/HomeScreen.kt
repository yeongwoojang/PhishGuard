package com.example.phishguard.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phishguard.domain.model.RiskLevel
import com.example.phishguard.domain.model.ThreatResult
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryBlue = Color(0xFF185FA5)
private val DangerRed = Color(0xFFE24B4A)
private val DangerRedLight = Color(0xFFFCEBEB)
private val DangerRedDark = Color(0xFFA32D2D)
private val CautionAmber = Color(0xFFEF9F27)
private val CautionAmberLight = Color(0xFFFAEEDA)
private val CautionAmberDark = Color(0xFF633806)
private val SafeGreen = Color(0xFF639922)
private val SafeGreenLight = Color(0xFFEAF3DE)
private val SafeGreenDark = Color(0xFF27500A)

private data class RiskStyle(
    val bgColor: Color,
    val borderColor: Color,
    val titleColor: Color,
    val textColor: Color,
    val badgeColor: Color,
    val badgeText: String
)

private data class HistoryStyle(
    val barColor: Color,
    val badgeBg: Color,
    val badgeText: String,
    val badgeTextColor: Color
)

private fun getRiskStyle(riskLevel: RiskLevel): RiskStyle = when (riskLevel) {
    RiskLevel.DANGER -> RiskStyle(
        bgColor = DangerRedLight,
        borderColor = Color(0xFFF09595),
        titleColor = DangerRedDark,
        textColor = Color(0xFF791F1F),
        badgeColor = DangerRed,
        badgeText = "위험"
    )
    RiskLevel.CAUTION -> RiskStyle(
        bgColor = CautionAmberLight,
        borderColor = Color(0xFFFAC775),
        titleColor = CautionAmberDark,
        textColor = Color(0xFF412402),
        badgeColor = CautionAmber,
        badgeText = "주의"
    )
    RiskLevel.SAFE -> RiskStyle(
        bgColor = SafeGreenLight,
        borderColor = Color(0xFFC0DD97),
        titleColor = SafeGreenDark,
        textColor = Color(0xFF173404),
        badgeColor = SafeGreen,
        badgeText = "안전"
    )
}

private fun getHistoryStyle(riskLevel: RiskLevel): HistoryStyle = when (riskLevel) {
    RiskLevel.DANGER -> HistoryStyle(DangerRed, DangerRedLight, "위험", DangerRedDark)
    RiskLevel.CAUTION -> HistoryStyle(CautionAmber, CautionAmberLight, "주의", CautionAmberDark)
    RiskLevel.SAFE -> HistoryStyle(SafeGreen, SafeGreenLight, "안전", SafeGreenDark)
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val threatHistory by viewModel.threatHistory.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var inputSender by remember { mutableStateOf("") }

    val totalCount = threatHistory.size
    val dangerCount = threatHistory.count { it.riskLevel == RiskLevel.DANGER }
    val safeCount = threatHistory.count { it.riskLevel == RiskLevel.SAFE }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HeaderSection(totalCount, dangerCount, safeCount)
        }

        item {
            AnalysisInputSection(
                inputSender = inputSender,
                inputText = inputText,
                isLoading = uiState is HomeUiState.Loading,
                onSenderChange = { inputSender = it },
                onTextChange = { inputText = it },
                onAnalyze = {
                    if (inputText.isNotBlank()) {
                        viewModel.analyzeMessage(inputText, inputSender)
                    }
                }
            )
        }

        item {
            when (val state = uiState) {
                is HomeUiState.Success -> AnalysisResultSection(state.result)
                is HomeUiState.Error -> ErrorSection(state.message)
                else -> {}
            }
        }

        if (threatHistory.isNotEmpty()) {
            item {
                Text(
                    text = "탐지 이력",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF888780),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(threatHistory) { threat ->
                ThreatHistoryCard(threat)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun HeaderSection(total: Int, danger: Int, safe: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlue)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PhishGuard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "AI 기반 피싱 문자 탐지",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = total.toString(),
                    label = "전체",
                    valueColor = Color.White
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = danger.toString(),
                    label = "위험",
                    valueColor = Color(0xFFFF6B6B)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = safe.toString(),
                    label = "안전",
                    valueColor = Color(0xFF51CF66)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    valueColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AnalysisInputSection(
    inputSender: String,
    inputText: String,
    isLoading: Boolean,
    onSenderChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "수동 분석",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888780)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputSender,
                onValueChange = onSenderChange,
                label = { Text("발신자 번호", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                label = { Text("문자 내용", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Text(
                    text = if (isLoading) "분석 중..." else "피싱 분석하기",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AnalysisResultSection(result: ThreatResult) {
    val style = getRiskStyle(result.riskLevel)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = style.bgColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(0.5.dp, style.borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(style.badgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${style.badgeText} 문자 탐지",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = style.titleColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.badgeColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${(result.riskScore * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.reason,
                fontSize = 12.sp,
                color = style.textColor,
                lineHeight = 18.sp
            )

            if (result.isGeminiAnalyzed) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.borderColor.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "AI 심층 분석",
                        fontSize = 11.sp,
                        color = style.titleColor
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ErrorSection(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DangerRedLight),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Text(
            text = "오류: $message",
            fontSize = 13.sp,
            color = DangerRedDark,
            modifier = Modifier.padding(14.dp)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ThreatHistoryCard(threat: ThreatResult) {
    val style = getHistoryStyle(threat.riskLevel)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .background(style.barColor)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = threat.messageText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C2C2A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatSender(threat.sender)} · ${formatTime(threat.analyzedAt)}",
                        fontSize = 11.sp,
                        color = Color(0xFF888780)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = style.badgeText,
                        fontSize = 11.sp,
                        color = style.badgeTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "방금 전"
        diff < 3600_000 -> "${diff / 60_000}분 전"
        diff < 86400_000 -> "${diff / 3600_000}시간 전"
        else -> SimpleDateFormat("MM/dd", Locale.KOREA).format(Date(timestamp))
    }
}

fun formatSender(sender: String): String {
    return if (sender.contains(".")) {
        "알 수 없음"  // 패키지명이면 대체
    } else {
        sender
    }
}