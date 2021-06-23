package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import java.lang.Exception

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeAndroidRepository(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {


    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }

        reminders?.let { return Result.Success(ArrayList(it)) }


        return Result.Error("Reminders not found")

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }

        if (reminders == null) {
            return Result.Error("The list is empty")
        } else {
            val theReminder = reminders!!.firstOrNull{it.id == id}
            theReminder?.let {  return Result.Success(it)}
            return Result.Error("id not found in the list")
        }

    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}