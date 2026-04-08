package com.example.phishguard.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phishguard.R
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
    onThreatClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val messageTestState by viewModel.messageTestState.collectAsState()
    val threatHistory by viewModel.threatHistory.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var inputSender by remember { mutableStateOf("") }

    val totalCount = threatHistory.size
    val dangerCount = threatHistory.count { it.riskLevel == RiskLevel.DANGER }
    val safeCount = threatHistory.count { it.riskLevel == RiskLevel.SAFE }

    //_ 전체 삭제 확인 다이얼로그
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("전체 삭제") },
            text = { Text("탐지 이력을 전부 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllThreats()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("삭제", color = Color(0xFFE24B4A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HeaderSection(
                total = totalCount,
                danger = dangerCount,
                safe = safeCount,
                onDeleteAll = { showDeleteAllDialog = true }  // 추가
            )
        }

        item {
            AnalysisInputSection(
                inputSender = inputSender,
                inputText = inputText,
                isLoading = messageTestState is HomeUiState.Loading,
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
            when (val state = messageTestState) {
                is HomeUiState.Success -> AnalysisResultSection(state.result)
                is HomeUiState.Error -> ErrorSection(state.message)
                else -> {}
            }
        }

        if (threatHistory.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.threat_history),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF888780),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(threatHistory) { threat ->
                ThreatHistoryCard(
                    threat = threat,
                    onClick = { onThreatClick(threat.id) },
                    onDelete = { viewModel.deleteThreat(threat.id) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun HeaderSection(
    total: Int,
    danger: Int,
    safe: Int,
    onDeleteAll: () -> Unit  // 추가
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlue)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                //_ 전체 삭제 버튼
                if (total > 0) {
                    TextButton(onClick = onDeleteAll) {
                        Text(
                            text = "전체 삭제",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //_ 통계 카드 기존 코드 그대로
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(Modifier.weight(1f), total.toString(), "전체", Color.White)
                StatCard(Modifier.weight(1f), danger.toString(), "위험", Color(0xFFFF6B6B))
                StatCard(Modifier.weight(1f), safe.toString(), "안전", Color(0xFF51CF66))
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
                text = stringResource(R.string.manual_analysis),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888780)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputSender,
                onValueChange = onSenderChange,
                label = { Text(stringResource(R.string.hint_sender), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.hint_message), fontSize = 13.sp) },
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
                    text = if (isLoading) stringResource(R.string.btn_authenticating) else stringResource(R.string.btn_analyze),
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
                        text = "${style.badgeText} ${stringResource(R.string.message_detection)}",
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
                        text = stringResource(R.string.label_ai_analysis),
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
            text = "${stringResource(R.string.error_prefix)}: $message",
            fontSize = 13.sp,
            color = DangerRedDark,
            modifier = Modifier.padding(14.dp)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ThreatHistoryCard(
    threat: ThreatResult,
    onClick: () -> Unit,
    onDelete: () -> Unit  // 추가
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,  //_ 오른쪽 → 왼쪽 스와이프만
        backgroundContent = {
            //_ 스와이프 시 빨간 배경 표시
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(Color(0xFFE24B4A)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "삭제",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        //_ 기존 카드 UI 그대로
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onClick() }
        ) {
            val style = getHistoryStyle(threat.riskLevel)

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
        "알 수 없음"  //_ 패키지명이면 알 수 없음 표시 (간혹 다른 앱에서 보내는 경우가 있음. 일단 알 수 없음 처리.)
    } else {
        sender
    }
}