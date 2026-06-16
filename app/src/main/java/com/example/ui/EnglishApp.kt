package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WordCard



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishApp(viewModel: EnglishViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val allWords by viewModel.allWords.collectAsStateWithLifecycle()
    val activeLevel by viewModel.activeLevel.collectAsStateWithLifecycle()
    val ttsReady by viewModel.isTtsReady.collectAsStateWithLifecycle()
    val suggestError by viewModel.suggestError.collectAsStateWithLifecycle()
    val isGeneratingSuggest by viewModel.isGeneratingSuggest.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageNameFilter(Icons.Default.Home),
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "MiniApps",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Learn English",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; viewModel.exitQuiz() },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Lessons") },
                    label = { Text("Lessons") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; viewModel.startQuiz(activeLevel) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Practice") },
                    label = { Text("Practice") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; viewModel.exitQuiz() },
                    icon = { Icon(Icons.Default.List, contentDescription = "Dictionary") },
                    label = { Text("Word Bank") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Word")
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Tab Routing
                when (selectedTab) {
                    0 -> LessonsScreen(
                        viewModel = viewModel,
                        allWords = allWords,
                        activeLevel = activeLevel
                    )
                    1 -> PracticeQuizScreen(
                        viewModel = viewModel
                    )
                    2 -> WordBankScreen(
                        viewModel = viewModel,
                        allWords = allWords,
                        activeLevel = activeLevel,
                        isGeneratingSuggest = isGeneratingSuggest,
                        suggestError = suggestError,
                        onClearError = { viewModel.clearSuggestError() }
                    )
                }
            }
        }
    }

    // Modal dialog to add word
    if (showAddDialog) {
        AddWordDialog(
            isGenerating = isGeneratingSuggest,
            onDismiss = { showAddDialog = false },
            onAddManual = { word, translation, phonetic, lvl, sentence, transSentence ->
                viewModel.addWordManually(word, translation, phonetic, lvl, sentence, transSentence)
                showAddDialog = false
            },
            onAddAi = { word, lvl ->
                viewModel.generateAndAddWord(word, lvl)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LessonsScreen(
    viewModel: EnglishViewModel,
    allWords: List<WordCard>,
    activeLevel: String
) {
    val levelWords = allWords.filter { it.levelName.equals(activeLevel, ignoreCase = true) }
    var currentCardIndex by remember { mutableStateOf(0) }

    // Correct index bounds immediately when words change
    LaunchedEffect(levelWords) {
        currentCardIndex = 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Level Picker row
        LevelSelectorRow(
            activeLevel = activeLevel,
            onLevelSelected = { viewModel.changeActiveLevel(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (levelWords.isEmpty()) {
            EmptyListPlaceholder("No words in this level yet. Go to Word Bank to add your first word manually or with AI!")
        } else {
            val safeIndex = currentCardIndex.coerceIn(0, levelWords.size - 1)
            val currentWord = levelWords[safeIndex]

            Text(
                text = "LEVEL LESSON: $activeLevel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Text(
                text = "Tap Card to Flip  •  Keep Practicing!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // FlashCard Section with local flip state key
            key(currentWord.id) {
                WordFlashCard(
                    wordCard = currentWord,
                    onPronounce = { viewModel.pronounceWord(currentWord.word) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Carousel Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (currentCardIndex > 0) currentCardIndex-- },
                    enabled = currentCardIndex > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Previous")
                }

                Text(
                    text = "${safeIndex + 1} of ${levelWords.size}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { if (currentCardIndex < levelWords.size - 1) currentCardIndex++ },
                    enabled = currentCardIndex < levelWords.size - 1,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Next")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Quiz trigger
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.startQuiz(activeLevel) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Quiz",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test Your Knowledge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Take a 5-question multi-choice quiz of $activeLevel level words.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play"
                    )
                }
            }
        }
    }
}

@Composable
fun WordFlashCard(
    wordCard: WordCard,
    onPronounce: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "カードフリップ"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(28.dp)
            )
            .clickable { isFlipped = !isFlipped },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            // FRONT SIDE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY LESSON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = wordCard.levelName.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Middle Content Area
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = wordCard.word,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = wordCard.pronunciationGuide,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { alpha = 0.8f }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }

                // Bottom Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onPronounce()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Listen",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Listen", fontWeight = FontWeight.Medium)
                    }

                    OutlinedButton(
                        onClick = { isFlipped = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Flip",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Translate", fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            // BACK SIDE (Mirrored Y)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .graphicsLayer { rotationY = 180f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRANSLATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        letterSpacing = 0.5.sp
                    )

                    IconButton(
                        onClick = { isFlipped = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Flip Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Middle Content Area
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = wordCard.translation,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        modifier = Modifier.width(60.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "EXAMPLE SENTENCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "\"${wordCard.exampleSentence}\"",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = wordCard.exampleTranslation,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Flip Back helper indicator
                Text(
                    text = "Tap card to flip back",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun LevelSelectorRow(
    activeLevel: String,
    onLevelSelected: (String) -> Unit
) {
    val levels = listOf("Beginner", "Intermediate", "Advanced")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            text = "LEVELS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            levels.forEach { level ->
                val isSelected = activeLevel == level
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onLevelSelected(level) }
                        .then(
                            if (!isSelected) {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PracticeQuizScreen(viewModel: EnglishViewModel) {
    val quizState by viewModel.quizScreenState.collectAsStateWithLifecycle()
    val questions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.quizCurrentIndex.collectAsStateWithLifecycle()
    val options by viewModel.quizOptions.collectAsStateWithLifecycle()
    val correctValue by viewModel.quizCorrectAnswer.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedAnswerIndex.collectAsStateWithLifecycle()
    val correctCount by viewModel.quizCorrectCount.collectAsStateWithLifecycle()
    val activeLevel by viewModel.activeLevel.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (quizState) {
            QuizState.NOT_STARTED -> {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Quiz Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Práctica de Vocabulario",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Test your $activeLevel vocabulary with high-speed quizzes!",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.startQuiz(activeLevel) },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Start Level Quiz")
                }
            }
            QuizState.NOT_ENOUGH_WORDS -> {
                Text("Not Enough Words", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This level requires at least 2 words to make a quiz. Add more words first!",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.exitQuiz() }) {
                    Text("Go Back")
                }
            }
            QuizState.IN_PROGRESS -> {
                val currentQuestion = questions[currentIndex]
                
                // Header progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${questions.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Score: ${correctCount}/${questions.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / questions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // The word to translate card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "What is the translation of:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentQuestion.word,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentQuestion.pronunciationGuide,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        IconButton(
                            onClick = { viewModel.pronounceWord(currentQuestion.word) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Listen voice",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Options (Multiple choice list)
                options.forEachIndexed { index, option ->
                    val isSelected = selectedIndex == index
                    val isCorrectValue = option == correctValue
                    val isAlreadyAnswered = selectedIndex != null

                    val cardColor = when {
                        isSelected && isCorrectValue -> Color(0xFF10B981) // Green success
                        isSelected && !isCorrectValue -> Color(0xFFEF4444) // Red danger
                        isAlreadyAnswered && isCorrectValue -> Color(0xFF10B981) // Highlight correct
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val textColor = when {
                        isAlreadyAnswered && (isCorrectValue || isSelected) -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isAlreadyAnswered) {
                                viewModel.submitAnswer(index)
                            },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = if (!isAlreadyAnswered) BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant) else null
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = option,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Action buttons
                AnimatedVisibility(
                    visible = selectedIndex != null,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(if (currentIndex < questions.size - 1) "Next Question" else "Finish Quiz")
                    }
                }
            }
            QuizState.FINISHED -> {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Trophy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(82.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Lesson Quiz Finished!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Congratulations! You correctly answered",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "$correctCount / ${questions.size} Words",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                val reviewText = when (correctCount) {
                    questions.size -> "Perfect score! You are mastering $activeLevel vocabulary! 🌟"
                    in 3..4 -> "Great job! Keep learning to get the flawless win! 👍"
                    else -> "No worries! Keep practicing, you'll reach fluency soon! 💪"
                }

                Text(
                    text = reviewText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row {
                    OutlinedButton(
                        onClick = { viewModel.exitQuiz() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Exit")
                    }
                    Button(
                        onClick = { viewModel.startQuiz(activeLevel) }
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordBankScreen(
    viewModel: EnglishViewModel,
    allWords: List<WordCard>,
    activeLevel: String,
    isGeneratingSuggest: Boolean,
    suggestError: String?,
    onClearError: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val filteredList = allWords.filter {
        it.word.contains(searchQuery, ignoreCase = true) ||
        it.translation.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Personal Vocabulary Base",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Total Words: ${allWords.size} (${allWords.filter { it.isCustom }.size} added by you)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search word or translation...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System notification / Error bar
        if (suggestError != null) {
            Snackbar(
                action = {
                    TextButton(onClick = { onClearError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                modifier = Modifier.padding(bottom = 12.dp),
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text(suggestError)
            }
        }

        if (isGeneratingSuggest) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Gemini AI is analyzing & generating phonetic guides...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            EmptyListPlaceholder("No words match your search. Add custom words using the (+) button.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList, key = { it.id }) { card ->
                    Row(
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        WordBankItem(
                            card = card,
                            onPronounce = { viewModel.pronounceWord(card.word) },
                            onDelete = { viewModel.deleteWord(card) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordBankItem(
    card: WordCard,
    onPronounce: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (card.levelName) {
        "Beginner" -> MaterialTheme.colorScheme.primary
        "Intermediate" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio button
            IconButton(
                onClick = { onPronounce() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Pronounce",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.word,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Level badge
                    Text(
                        text = card.levelName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    if (card.isCustom) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI/Custom",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${card.pronunciationGuide}  •  ${card.translation}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (card.exampleSentence.isNotEmpty()) {
                    Text(
                        text = "\"${card.exampleSentence}\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Delete button
            IconButton(onClick = { onDelete() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyListPlaceholder(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "No result",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onAddManual: (String, String, String, String, String, String) -> Unit,
    onAddAi: (String, String) -> Unit
) {
    var wordText by remember { mutableStateOf("") }
    var translationText by remember { mutableStateOf("") }
    var phoneticText by remember { mutableStateOf("") }
    var levelSelected by remember { mutableStateOf("Beginner") }
    var sentenceText by remember { mutableStateOf("") }
    var sentenceTranslation by remember { mutableStateOf("") }

    var useAiAutoFill by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Attach New Word",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher between AI generator or Manual mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (useAiAutoFill) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { useAiAutoFill = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "AI Generation",
                            color = if (useAiAutoFill) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!useAiAutoFill) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { useAiAutoFill = false }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Manual Form",
                            color = if (!useAiAutoFill) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Word Input
                OutlinedTextField(
                    value = wordText,
                    onValueChange = { wordText = it },
                    label = { Text("English Word") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Level Picker
                Text("Select Level:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                        FilterChip(
                            selected = levelSelected == level,
                            onClick = { levelSelected = level },
                            label = { Text(level, fontSize = 11.sp) }
                        )
                    }
                }

                if (useAiAutoFill) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "✨ Gemini AI will automatically generate Spanish translations, accurate phonetics guide, and clear example sentences for this word.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 16.sp
                    )
                } else {
                    // Manual Content fields
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = translationText,
                        onValueChange = { translationText = it },
                        label = { Text("Spanish Translation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneticText,
                        onValueChange = { phoneticText = it },
                        label = { Text("Phonetic /Pronunciation/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sentenceText,
                        onValueChange = { sentenceText = it },
                        label = { Text("Example Sentence (English)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sentenceTranslation,
                        onValueChange = { sentenceTranslation = it },
                        label = { Text("Example Translation (Spanish)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (wordText.isNotBlank()) {
                                if (useAiAutoFill) {
                                    onAddAi(wordText, levelSelected)
                                } else {
                                    onAddManual(
                                        wordText,
                                        translationText,
                                        phoneticText,
                                        levelSelected,
                                        sentenceText,
                                        sentenceTranslation
                                    )
                                }
                            }
                        },
                        enabled = wordText.isNotBlank() && !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Text("Save Word")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to ensure standard Android Icons resolve completely.
 */
fun imageNameFilter(icon: androidx.compose.ui.graphics.vector.ImageVector): androidx.compose.ui.graphics.vector.ImageVector {
    return icon
}
