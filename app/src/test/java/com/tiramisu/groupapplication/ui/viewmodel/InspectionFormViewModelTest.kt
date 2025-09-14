package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.firebase.auth.FirebaseUser
import com.tiramisu.groupapplication.domain.enums.InspectionType
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import com.tiramisu.groupapplication.domain.repository.AuthRepository
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.domain.repository.LogRepository
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel.UiEvent
import com.tiramisu.groupapplication.util.AttachmentLinkResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
// CHANGED: Import StandardTestDispatcher instead of UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class InspectionFormViewModelMockKTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var inspectionRepo: InspectionRepository
    @MockK
    private lateinit var attachmentRepo: AttachmentRepository
    @MockK(relaxed = true)
    private lateinit var logRepo: LogRepository
    @MockK
    private lateinit var authRepo: AuthRepository
    @MockK
    private lateinit var linkResolver: AttachmentLinkResolver

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: InspectionFormViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testUserId = "test-user-123"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle()
        val mockFirebaseUser: FirebaseUser = mockk()
        every { mockFirebaseUser.uid } returns testUserId
        every { authRepo.currentUser } returns flowOf(mockFirebaseUser)

        viewModel = InspectionFormViewModel(
            repo = inspectionRepo,
            attachmentRepo = attachmentRepo,
            logRepo = logRepo,
            savedStateHandle = savedStateHandle,
            linkResolver = linkResolver,
            auth = authRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save - for new inspection, then upserts and emits Saved event`() = runTest {
        // Arrange
        populateValidFormData()
        coEvery { inspectionRepo.upsertLocal(any()) } returns Unit

        // Act
        viewModel.save()

        // Assert
        viewModel.events.test {
            val event = awaitItem()
            // This test now passes because the ViewModel emits the event
            assertTrue(event is UiEvent.Saved)
        }

        coVerify { inspectionRepo.upsertLocal(withArg {
            assertEquals(testUserId, it.createdBy)
        })}
    }

    // --- Helper Methods ---
    private fun populateValidFormData() {
        viewModel.setType(InspectionType.QUALITY)
        viewModel.setTitle("Valid Title")
        viewModel.setNotes("Some valid notes.")
        viewModel.setScore("95")
        viewModel.setOutcome("Pass with distinction")
    }
}