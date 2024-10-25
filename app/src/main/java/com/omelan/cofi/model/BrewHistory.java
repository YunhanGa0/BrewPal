package com.omelan.cofi.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "brew_history")
public class BrewHistory {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long recipeId;
    private Date brewDate;
    private double beansUsed;
    private String notes;

    public BrewHistory(long recipeId, Date brewDate, double beansUsed, String notes) {
        this.recipeId = recipeId;
        this.brewDate = brewDate;
        this.beansUsed = beansUsed;
        this.notes = notes;
    }

    // Getters and setters
    // ...
}
