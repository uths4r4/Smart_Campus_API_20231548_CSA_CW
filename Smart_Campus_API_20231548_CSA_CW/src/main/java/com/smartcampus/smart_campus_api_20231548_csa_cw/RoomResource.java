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

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Context
    private UriInfo uriInfo;

    // GET /api/v1/rooms — Provide a comprehensive list of all rooms [cite: 115]
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(DataStore.getRooms().values());
        return Response.ok(roomList).build();
    }

    // POST /api/v1/rooms — Enable the creation of new rooms [cite: 116]
    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Room ID is required\"}")
                    .build();
        }
        
        if (DataStore.getRoom(room.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"Room with this ID already exists\"}")
                    .build();
        }

        DataStore.addRoom(room);

        // FIX: Build the URI for the Location header required by Rubric 2.1 
        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        
        return Response.created(location).entity(room).build();
    }

    // GET /api/v1/rooms/{roomId} — Fetch detailed metadata for a specific room [cite: 117]
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room not found\"}")
                    .build();
        }
        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId} — Room decommissioning [cite: 121]
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room not found\"}")
                    .build();
        }

        // Business Logic Constraint: Block deletion if active sensors exist 
        // This triggers the RoomNotEmptyException mapped to 409 Conflict [cite: 153]
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room '" + roomId + "' cannot be deleted as it still has sensors assigned to it.");
        }

        DataStore.deleteRoom(roomId);
        return Response.noContent().build();
    }
}