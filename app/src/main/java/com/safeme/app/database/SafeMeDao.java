package com.safeme.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SafeMeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertJourneys(List<CachedJourneyEntity> journeys);

    @Query("SELECT * FROM cached_journeys ORDER BY expectedArrivalTime DESC")
    List<CachedJourneyEntity> getAllCachedJourneys();

    @Query("SELECT * FROM cached_journeys WHERE status = 'ACTIVE' LIMIT 1")
    CachedJourneyEntity getCachedActiveJourney();

    @Query("DELETE FROM cached_journeys")
    void clearJourneys();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertContacts(List<CachedContactEntity> contacts);

    @Query("SELECT * FROM cached_contacts ORDER BY priority ASC")
    List<CachedContactEntity> getAllCachedContacts();

    @Query("DELETE FROM cached_contacts")
    void clearContacts();
}
