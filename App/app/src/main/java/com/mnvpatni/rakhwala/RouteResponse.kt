package com.mnvpatni.rakhwala

data class RouteResponse(
    val statusCode: Int,
    val body: List<Route>
)

data class Route(
    val route_index: Int,
    val distance: Int,
    val duration: Int,
    val safety: String,
    val traffic_info: String,
    val police_stations: Int,
    val hospitals: Int,
    val directions: List<Direction>,
    val start_point: Location,
    val end_point: Location
)

data class Direction(
    val summary: Summary,
    val points: List<Location>
)

data class Summary(
    val lengthInMeters: Int,
    val travelTimeInSeconds: Int,
    val departureTime: String,
    val arrivalTime: String
)

data class Location(
    val latitude: Double,
    val longitude: Double
)
