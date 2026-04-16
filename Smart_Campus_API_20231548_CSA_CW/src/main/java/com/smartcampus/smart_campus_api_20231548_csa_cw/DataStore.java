/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.smart_campus_api_20231548_csa_cw;

/**
 *
 * @author uthsarak
 */

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataStore {

    private static final Map<String, Room> rooms = new LinkedHashMap<>();
    private static final Map<String, Sensor> sensors = new LinkedHashMap<>();
    private static final Map<String, List<SensorReading>> sensorReadings = new LinkedHashMap<>();

    // Room methods
    public static Map<String, Room> getRooms() { return rooms; }

    public static Room getRoom(String id) { return rooms.get(id); }

    public static void addRoom(Room room) { rooms.put(room.getId(), room); }

    public static boolean deleteRoom(String id) {
        if (rooms.containsKey(id)) {
            rooms.remove(id);
            return true;
        }
        return false;
    }

    // Sensor methods
    public static Map<String, Sensor> getSensors() { return sensors; }

    public static Sensor getSensor(String id) { return sensors.get(id); }

    public static void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Also register sensorId in the Room
        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().add(sensor.getId());
        }
        // Initialise empty reading list for this sensor
        sensorReadings.put(sensor.getId(), new ArrayList<>());
    }

    public static boolean deleteSensor(String id) {
        if (sensors.containsKey(id)) {
            Sensor sensor = sensors.get(id);
            // Remove sensorId from the Room
            Room room = rooms.get(sensor.getRoomId());
            if (room != null) {
                room.getSensorIds().remove(id);
            }
            sensors.remove(id);
            sensorReadings.remove(id);
            return true;
        }
        return false;
    }

    // SensorReading methods
    public static List<SensorReading> getReadings(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, new ArrayList<>());
    }

    public static void addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}