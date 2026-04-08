package com.example.phishguard.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phishguard.domain.model.RiskLevel
import com.example.phishguard.domain.model.ThreatResult
import java.text.SimpleDateFormat
import java.util.*

private val DangerRed = Color(0xFFE24B4A)
private val DangerRedLight = Color(0xFFFCEBEB)
private val DangerRedDark = Color(0xFFA32D2D)
private val CautionAmber = Color(0xFFEF9F27)
private val CautionAmberLight = Color(0xFFFAEEDA)
private val CautionAmberDark = Color(0xFF633806)
private val SafeGreen = Color(0xFF639922)
private val SafeGreenLight = Color(0xFFEAF3DE)
private val SafeGreenDark = Color(0xFF27500A)
private val PrimaryBlue = Color(0xFF185FA5)

private data class RiskStyle(
    val bgColor: Color,
    val borderColor: Color,
    val titleColor: Color,
    val textColor: Color,
    val badgeColor: Color,
    val badgeText: String
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    threatId: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val threat by viewModel.threat.collectAsState()

    LaunchedEffect(threatId) {
        viewModel.loadThreat(threatId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "상세 정보",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        threat?.let { result ->
            DetailContent(
                result = result,
                modifier = Modifier.padding(paddingValues)
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    }
}

@Composable
fun DetailContent(
    result: ThreatResult,
    modifier: Modifier = Modifier
) {
    val style = getRiskStyle(result.riskLevel)  // 교체

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //_ 위험도 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = style.bgColor),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, style.borderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(style.bgColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${style.badgeText} 문자",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = style.titleColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(style.badgeColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(result.riskScore * 100).toInt()}%",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = result.reason,
                    fontSize = 14.sp,
                    color = style.textColor,
                    lineHeight = 22.sp
                )

                if (result.isGeminiAnalyzed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background((style.borderColor).copy(alpha = 0.4f))
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

        //_ 발신자 정보 카드
        DetailInfoCard(
            title = "발신자",
            content = result.sender
        )

        //_ 수신 시각 카드
        DetailInfoCard(
            title = "수신 시각",
            content = SimpleDateFormat(
                "yyyy년 MM월 dd일 HH:mm:ss",
                Locale.KOREA
            ).format(Date(result.analyzedAt))
        )

        //_ 문자 내용 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "문자 내용",
                    fontSize = 12.sp,
                    color = Color(0xFF888780),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.messageText,
                    fontSize = 14.sp,
                    color = Color(0xFF2C2C2A),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun DetailInfoCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF888780),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = content,
                fontSize = 13.sp,
                color = Color(0xFF2C2C2A)
            )
        }
    }
}