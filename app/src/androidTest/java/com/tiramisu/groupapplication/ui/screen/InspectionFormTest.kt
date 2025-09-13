package com.tiramisu.groupapplication.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormData
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InspectionFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel: InspectionFormViewModel = mockk(relaxed = true)

    @Before
    fun setup() {
        // GIVEN: A real StateFlow for the mock ViewModel's formData.
        val fakeFormDataState = MutableStateFlow(InspectionFormData())
        every { mockViewModel.formData } returns fakeFormDataState

        // ADD THIS: GIVEN a real (but empty) SharedFlow for the mock ViewModel's events.
        // This prevents the KotlinNothingValueException inside LaunchedEffect.
        val fakeEventsFlow = MutableSharedFlow<InspectionFormViewModel.UiEvent>()
        every { mockViewModel.events } returns fakeEventsFlow
    }

    @Test
    fun fillFormAndClickSave_invokesViewModelSave() {
        // Given: The InspectionForm is on screen
        composeTestRule.setContent {
            InspectionForm(
                viewModel = mockViewModel,
                onBack = { },
                onSaved = { },
                navController = rememberNavController()
            )
        }

        // When: The user fills in all the required fields
        composeTestRule.onNodeWithTag("type_input")
            .performTextInput("Routine Maintenance")
        composeTestRule.onNodeWithTag("title_input")
            .performTextInput("HVAC Unit Check")
        composeTestRule.onNodeWithTag("notes_input")
            .performTextInput("Filter replaced and coils cleaned.")
        composeTestRule.onNodeWithTag("score_input")
            .performTextInput("92")
        composeTestRule.onNodeWithTag("outcome_input")
            .performTextInput("Pass")

        // And: The user clicks the save button
        composeTestRule.onNodeWithTag("save_button").performClick()

        // Then: The ViewModel's save method should be called
        verify(exactly = 1) { mockViewModel.save() }
    }
}