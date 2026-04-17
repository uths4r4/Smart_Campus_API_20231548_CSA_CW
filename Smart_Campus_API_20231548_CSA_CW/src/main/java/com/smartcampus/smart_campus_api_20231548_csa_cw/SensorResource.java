/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.smart_campus_api_20231548_csa_cw;

/**
 *
 * @author uthsarak
 */

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @Context
    private UriInfo uriInfo;

    /**
     * GET /api/v1/sensors
     * Supports optional filtering by type via QueryParam.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensorList = new ArrayList<>(DataStore.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            List<Sensor> filtered = new ArrayList<>();
            for (Sensor s : sensorList) {
                if (s.getType().equalsIgnoreCase(type)) {
                    filtered.add(s);
                }
            }
            return Response.ok(filtered).build();
        }

        return Response.ok(sensorList).build();
    }

    /**
     * POST /api/v1/sensors
     * Includes validation for Room ID existence
     * and returns a Location header for the new resource.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Sensor ID is required\"}")
                    .build();
        }
        
        if (DataStore.getSensor(sensor.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"Sensor with this ID already exists\"}")
                    .build();
        }

        // Validate that the roomId exists - Throws custom exception for 422/400 mapping
        if (sensor.getRoomId() == null || DataStore.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException("Room with ID '" + sensor.getRoomId() + "' does not exist.");
        }

        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        DataStore.addSensor(sensor);

        // Build the Location header URI for the new sensor
        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        
        return Response.created(location).entity(sensor).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Sensor not found\"}")
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * SUB-RESOURCE LOCATOR
     * No HTTP method annotation (@GET/@POST) here. 
     * This delegates all requests starting with {sensorId}/readings to SensorReadingResource.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        // First, verify the sensor exists before delegating
        if (DataStore.getSensor(sensorId) == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Parent Sensor not found\"}")
                        .build()
            );
        }
        return new SensorReadingResource(sensorId);
    }
}
