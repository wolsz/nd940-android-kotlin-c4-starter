package com.udacity.project4.locationreminders.savereminder


import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.MainCoroutineRule
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {


    //TODO: provide testing to the SaveReminderView and its live data objects

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private val appContext: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun  setupTheViewModel() {
        val reminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(appContext, reminderDataSource)
    }

    @After
    fun tearDown() { stopKoin() }


    @Test
    fun onClear_setToNullOrFalse() {
        saveReminderViewModel.onClear()

        val reminderTitle: String? = saveReminderViewModel.reminderTitle.getOrAwaitValue()
        assertThat(reminderTitle, nullValue())

        val reminderDescription: String? = saveReminderViewModel.reminderDescription.getOrAwaitValue()
        assertThat(reminderDescription, nullValue())

        val reminderSelectedLocationStr: String? = saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue()
        assertThat(reminderSelectedLocationStr, nullValue())

        val selectedPOI: PointOfInterest? = saveReminderViewModel.selectedPOI.getOrAwaitValue()
        assertThat(selectedPOI, nullValue())

        val latitude: Double? = saveReminderViewModel.latitude.getOrAwaitValue()
        assertThat(latitude, nullValue())

        val longitude: Double? = saveReminderViewModel.longitude.getOrAwaitValue()
        assertThat(longitude, nullValue())

        val isSelected: Boolean? = saveReminderViewModel.isSelected.getOrAwaitValue()
        assertThat(isSelected, `is`(false))
    }


    @Test
    fun validateEnteredData_emptyTitle_error() {
        val reminderDataItem = ReminderDataItem("", "", "location", 0.0, 0.0)

        saveReminderViewModel.validateEnteredData(reminderDataItem)
        val errorValue: Int? = saveReminderViewModel.showSnackBarInt.value

        assertThat(errorValue, `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_emptyLocation_error() {
        val reminderDataItem = ReminderDataItem("title", "", "", 0.0, 0.0)

        saveReminderViewModel.validateEnteredData(reminderDataItem)
        val errorValue: Int? = saveReminderViewModel.showSnackBarInt.value

        assertThat(errorValue, `is`(R.string.err_select_location))
    }

    @Test
    fun saveReminder_validatedData_savedInDatabase() {
        val reminderDataItem = ReminderDataItem("title", "description", "location", 0.0, 0.0)

        saveReminderViewModel.saveReminder(reminderDataItem)
        val result: String? = saveReminderViewModel.showToast.value

        assertThat(result, `is`(appContext.resources.getString(R.string.reminder_saved)))
    }

    @Test
    fun saveReminders_loading() {
        val reminderDataItem = ReminderDataItem("", "", "location", 0.0, 0.0)
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.saveReminder(reminderDataItem)

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    

}