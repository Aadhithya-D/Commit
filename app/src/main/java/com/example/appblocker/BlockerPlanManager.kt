package com.example.appblocker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class BlockerPlanManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("blocker_plans", Context.MODE_PRIVATE)
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .create()
    
    private companion object {
        const val KEY_CURRENT_PLAN = "current_plan"
        const val KEY_PLANS_LIST = "plans_list"
    }
    
    fun saveBlockerPlan(plan: BlockerPlan) {
        val json = gson.toJson(plan)
        sharedPreferences.edit()
            .putString(KEY_CURRENT_PLAN, json)
            .apply()
    }
    
    fun getCurrentBlockerPlan(): BlockerPlan? {
        val json = sharedPreferences.getString(KEY_CURRENT_PLAN, null)
        return if (json != null) {
            try {
                gson.fromJson(json, BlockerPlan::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    fun hasBlockerPlan(): Boolean {
        return getCurrentBlockerPlan() != null
    }
    
    fun deleteCurrentPlan() {
        sharedPreferences.edit()
            .remove(KEY_CURRENT_PLAN)
            .apply()
    }
    
    fun createPlanId(): String {
        return UUID.randomUUID().toString()
    }
    
    private class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_TIME
        
        override fun serialize(src: LocalTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalTime {
            return LocalTime.parse(json?.asString, formatter)
        }
    }
} 