package com.elena.autoplanner.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elena.autoplanner.data.local.TaskDatabase
import com.elena.autoplanner.data.repositories.ListRepositoryImpl
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ListWorkflowTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: TaskDatabase
    private lateinit var listRepository: ListRepositoryImpl
    private lateinit var saveListUseCase: SaveListUseCase
    private lateinit var getListsInfoUseCase: GetListsInfoUseCase
    private lateinit var saveSectionUseCase: SaveSectionUseCase
    private lateinit var getAllSectionsUseCase: GetAllSectionsUseCase
    private lateinit var deleteListUseCase: DeleteListUseCase

    @Mock
    private lateinit var userRepository: UserRepository
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testScope = TestScope(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()

        whenever(userRepository.getCurrentUser()).thenReturn(flowOf(null))

        listRepository = ListRepositoryImpl(
            listDao = database.listDao(),
            sectionDao = database.sectionDao(),
            taskDao = database.taskDao(),
            userRepository = userRepository,
            firestore = mock(),
            dispatcher = testDispatcher,
            repoScope = testScope
        )

        saveListUseCase = SaveListUseCase(listRepository)
        getListsInfoUseCase = GetListsInfoUseCase(listRepository)
        saveSectionUseCase = SaveSectionUseCase(listRepository)
        getAllSectionsUseCase = GetAllSectionsUseCase(listRepository)
        deleteListUseCase = DeleteListUseCase(listRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeListWorkflow() = runTest(testDispatcher) {
        val newList = TaskList(
            name = "Work Projects",
            colorHex = "#FF5722"
        )

        val saveResult = saveListUseCase(newList)
        assertTrue("List should save successfully", saveResult is TaskResult.Success)
        val listId = (saveResult as TaskResult.Success).data
        val listsInfo = getListsInfoUseCase().first()
        assertTrue("Should retrieve lists info", listsInfo is TaskResult.Success)
        val lists = (listsInfo as TaskResult.Success).data
        assertEquals("Should have 1 list", 1, lists.size)
        assertEquals("Work Projects", lists.first().list.name)
        assertEquals("#FF5722", lists.first().list.colorHex)
        val section1 = TaskSection(
            listId = listId,
            name = "High Priority",
            displayOrder = 1
        )
        val section1Result = saveSectionUseCase(section1)
        assertTrue("First section should save successfully", section1Result is TaskResult.Success)

        val section2 = TaskSection(
            listId = listId,
            name = "Low Priority",
            displayOrder = 2
        )
        val section2Result = saveSectionUseCase(section2)
        assertTrue("Second section should save successfully", section2Result is TaskResult.Success)
        val sectionsResult = getAllSectionsUseCase(listId)
        assertTrue("Should retrieve sections", sectionsResult is TaskResult.Success)
        val sections = (sectionsResult as TaskResult.Success).data
        assertEquals("Should have 2 sections", 2, sections.size)

        val sortedSections = sections.sortedBy { it.displayOrder }
        assertEquals("High Priority", sortedSections[0].name)
        assertEquals("Low Priority", sortedSections[1].name)
        val deleteResult = deleteListUseCase(listId)
        assertTrue("List deletion should succeed", deleteResult is TaskResult.Success)
        val remainingLists = getListsInfoUseCase().first()
        assertTrue("Should retrieve remaining lists", remainingLists is TaskResult.Success)
        assertEquals(
            "Should have 0 lists remaining",
            0,
            (remainingLists as TaskResult.Success).data.size
        )
        val remainingSections = getAllSectionsUseCase(listId)
        assertTrue("Should retrieve remaining sections", remainingSections is TaskResult.Success)
        assertEquals(
            "Should have 0 sections remaining",
            0,
            (remainingSections as TaskResult.Success).data.size
        )
    }

    @Test
    fun listValidationWorkflow() = runTest(testDispatcher) {
        val invalidList = TaskList(
            name = "",
            colorHex = "#FF0000"
        )

        val result = saveListUseCase(invalidList)
        assertTrue("Invalid list should return error", result is TaskResult.Error)
        assertEquals("List name cannot be empty.", (result as TaskResult.Error).message)
        val validList = TaskList(
            name = "Valid List",
            colorHex = "#00FF00"
        )

        val validResult = saveListUseCase(validList)
        assertTrue("Valid list should save successfully", validResult is TaskResult.Success)
    }

    @Test
    fun sectionValidationWorkflow() = runTest(testDispatcher) {
        val list = TaskList(name = "Test List", colorHex = "#0000FF")
        val listResult = saveListUseCase(list)
        assertTrue("List should save successfully", listResult is TaskResult.Success)
        val listId = (listResult as TaskResult.Success).data
        val invalidSection = TaskSection(
            listId = listId,
            name = ""
        )

        val result = saveSectionUseCase(invalidSection)
        assertTrue("Invalid section should return error", result is TaskResult.Error)
        assertEquals("Section name cannot be empty.", (result as TaskResult.Error).message)
        val validSection = TaskSection(
            listId = listId,
            name = "Valid Section"
        )

        val validResult = saveSectionUseCase(validSection)
        assertTrue("Valid section should save successfully", validResult is TaskResult.Success)
    }
}