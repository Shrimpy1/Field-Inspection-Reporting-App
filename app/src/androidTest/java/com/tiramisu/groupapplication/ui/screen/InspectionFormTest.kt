package com.tiramisu.groupapplication.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.tiramisu.groupapplication.domain.enums.InspectionType // Import the enum
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

    // Use relaxed = true so we don't have to mock every single function
    private val mockViewModel: InspectionFormViewModel = mockk(relaxed = true)

    // Keep a reference to the StateFlow to update it
    private lateinit var fakeFormDataState: MutableStateFlow<InspectionFormData>

    @Before
    fun setup() {
        // GIVEN: A real StateFlow for the mock ViewModel's formData.
        fakeFormDataState = MutableStateFlow(InspectionFormData())
        every { mockViewModel.formData } returns fakeFormDataState
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

        // When: The user interacts with the "Type" field
        // CHANGED: Instead of typing, we simulate a click,
        // which would open the picker in a real scenario.
        composeTestRule.onNodeWithTag("type_input").performClick()

        // For this test, we can directly call the ViewModel function
        // that would be triggered by the picker, to set the type state.
        // This isolates the test from the picker's implementation details.
        every { mockViewModel.setType(any()) } answers {
            fakeFormDataState.value = fakeFormDataState.value.copy(type = firstArg())
        }
        mockViewModel.setType(InspectionType.SITE_GENERAL) // Simulate selecting a type

        // And: The user fills in the rest of the fields
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