package com.example.data.local

import androidx.room.*
import com.example.data.model.FeeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeDao {
    @Query("SELECT * FROM fee_records ORDER BY lastUpdated DESC")
    fun getAllFeesFlow(): Flow<List<FeeRecord>>

    @Query("SELECT * FROM fee_records WHERE studentId = :studentId ORDER BY dueDate DESC")
    fun getFeesForStudentFlow(studentId: String): Flow<List<FeeRecord>>

    @Query("SELECT * FROM fee_records WHERE feeId = :feeId")
    suspend fun getFeeById(feeId: String): FeeRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFee(fee: FeeRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFees(fees: List<FeeRecord>)

    @Update
    suspend fun updateFee(fee: FeeRecord)

    @Delete
    suspend fun deleteFee(fee: FeeRecord)

    @Query("DELETE FROM fee_records")
    suspend fun clearAllFees()
}
