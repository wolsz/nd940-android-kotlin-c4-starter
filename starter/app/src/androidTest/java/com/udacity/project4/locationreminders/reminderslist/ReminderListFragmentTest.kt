package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidRepository
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }

            single { FakeAndroidRepository() as ReminderDataSource }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }

    }



    //    TODO: test the navigation of the fragments.
    @Test
    fun onFAB_whenClicked_navigateToSaveReminderFrag() = runBlockingTest {
        //Given
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        //Then
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        //When - click the fab
        onView(withId(R.id.addReminderFAB)).perform(click())
        //Check that the correct navigation is triggered.
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun loadData_testRecyclerView() = runBlockingTest {
        val reminder1 = ReminderDTO(
            "FirstTitle",
            "FirstDescription",
            "FirstLocation",
            0.0,
            0.0
        )

        repository.saveReminder(reminder1)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        onView(withText(reminder1.title)).check(matches(isDisplayed()))
        onView(withText(reminder1.description)).check(matches(isDisplayed()))
        onView(withText(reminder1.location)).check(matches(isDisplayed()))


    }

    @Test
    fun loadNoData_testNoDataDisplayed() = runBlockingTest {

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

    }

    @Test
    fun reminderListFragmentTest_whenThereIsError_shouldInUi() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)

        val fragmentScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)
        fragmentScenario.onFragment {
            it._viewModel.showSnackBar.postValue("There was an Error")
        }
        onView(withText("There was an Error")).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }
}