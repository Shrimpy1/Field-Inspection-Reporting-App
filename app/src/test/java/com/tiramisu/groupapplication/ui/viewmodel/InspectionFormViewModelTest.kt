package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.domain.repository.LogRepository
import com.tiramisu.groupapplication.util.AttachmentLinkResolver
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InspectionFormViewModelTest {

    // Mocks for all dependencies
    private lateinit var repo: InspectionRepository
    private lateinit var attachmentRepo: AttachmentRepository
    private lateinit var logRepo: LogRepository
    private lateinit var linkResolver: AttachmentLinkResolver
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: InspectionFormViewModel

    // Test dispatcher for managing coroutines
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Set the main dispatcher to our test dispatcher
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks before each test
        repo = mockk(relaxed = true)
        attachmentRepo = mockk(relaxed = true)
        logRepo = mockk(relaxed = true)
        linkResolver = mockk(relaxed = true)
        // Provide a default draftId
        savedStateHandle = SavedStateHandle(mapOf("draftInspectionId" to "test-draft-id"))

        viewModel = InspectionFormViewModel(
            repo,
            attachmentRepo,
            logRepo,
            savedStateHandle,
            linkResolver
        )
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher after each test
        Dispatchers.resetMain()
    }

    @Test
    fun `save with blank title updates form errors and does not save`() = runTest {
        // Given: The user has filled out some fields but left the title blank
        viewModel.setType("Routine Check")
        viewModel.setTitle("") // Blank title
        viewModel.setScore("85")
        viewModel.setOutcome("Pass")
        viewModel.setNotes("Some notes.")

        // When: The save button is clicked
        viewModel.save()
        // Advance coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: The form data should contain a title error
        val errors = viewModel.formData.value.errors
        assertNotNull("Title error should not be null", errors.title)
        assertEquals("Title is required", errors.title)
        assertTrue("Should have errors", errors.hasAny)

        // And: The repository's save method should NOT have been called
        coVerify(exactly = 0) { repo.upsertLocal(any()) }
    }

    @Test
    fun `save with non-numeric score updates form errors and does not save`() = runTest {
        // Given: The user entered text instead of a number for the score
        viewModel.setType("Routine Check")
        viewModel.setTitle("Valid Title")
        viewModel.setScore("invalid-score") // Non-numeric score
        viewModel.setOutcome("Pass")
        viewModel.setNotes("Some notes.")

        // When: The save button is clicked
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: The form data should contain a score error
        val errors = viewModel.formData.value.errors
        assertNotNull("Score error should not be null", errors.score)
        assertEquals("Score must be a number", errors.score)

        // And: The repository's save method should NOT have been called
        coVerify(exactly = 0) { repo.upsertLocal(any()) }
    }

    @Test
    fun `save with valid data calls repository and emits saved event`() = runTest {
        // Given: All fields are filled with valid data
        viewModel.setType("Annual Audit")
        viewModel.setTitle("Site Safety Inspection")
        viewModel.setNotes("All systems are nominal.")
        viewModel.setScore("100")
        viewModel.setOutcome("Excellent")

        // When: The save button is clicked
        viewModel.save()

        // Then: The event flow should emit a Saved event
        viewModel.events.test {
            val event = awaitItem()
            assertTrue("Event should be Saved", event is InspectionFormViewModel.UiEvent.Saved)
            assertEquals("test-draft-id", (event as InspectionFormViewModel.UiEvent.Saved).id)
            cancelAndIgnoreRemainingEvents()
        }

        // And: The repository upsert and log methods should be called exactly once
        coVerify(exactly = 1) { repo.upsertLocal(any()) }
        coVerify(exactly = 1) { logRepo.log("test-draft-id", "ADDED") }
    }
}