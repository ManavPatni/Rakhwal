import json
import requests
import random
from concurrent.futures import ThreadPoolExecutor

# Function to fetch routes from TomTom API
def get_routes_url(start_lat, start_lon, end_lat, end_lon, api_key):
    return f"https://api.tomtom.com/routing/1/calculateRoute/{start_lat},{start_lon}:{end_lat},{end_lon}/json?maxAlternatives=2&key={api_key}"

# Function to fetch nearby places (police stations, hospitals) for a route
def fetch_nearby_places(start_lat, start_lon, end_lat, end_lon, api_key):
    # Generate random counts for police and hospital stations between 0 and 5
    places_data = {
        "police": [None] * random.randint(0, 5),
        "hospital": [None] * random.randint(0, 5)
    }
    return places_data

# Function to calculate a combined score for routes based on safety and time
def calculate_route_score(route, places_data):
    police_count = len(places_data.get("police", []))
    hospital_count = len(places_data.get("hospital", []))
    duration = route["summary"]["travelTimeInSeconds"]
    
    # Calculate score
    safety_score = police_count * 2.0 + hospital_count * 1.5
    time_penalty = duration / 3600  # Time in hours

    # Higher score means a safer, faster route
    return safety_score - time_penalty

# Function to fetch routes from TomTom API
def fetch_routes(start_lat, start_lon, end_lat, end_lon, api_key):
    url = get_routes_url(start_lat, start_lon, end_lat, end_lon, api_key)
    response = requests.get(url)
    return response.json().get('routes', []) if response.status_code == 200 else []

# Lambda handler function
def lambda_handler(event, context):
    start_lat = event['start_lat']
    start_lon = event['start_lon']
    end_lat = event['end_lat']
    end_lon = event['end_lon']
    api_key = "YgtGPNvE2z1D6lnSn3BTWbGnJtLxMf3C"

    # Concurrently fetch routes and nearby places for efficiency
    with ThreadPoolExecutor() as executor:
        routes_future = executor.submit(fetch_routes, start_lat, start_lon, end_lat, end_lon, api_key)
        places_future = executor.submit(fetch_nearby_places, start_lat, start_lon, end_lat, end_lon, api_key)
    
    routes = routes_future.result()
    places_data = places_future.result()

    # Calculate and sort routes by score
    for route in routes:
        route["police_stations"] = len(places_data.get("police", []))
        route["hospitals"] = len(places_data.get("hospital", []))
        route["score"] = calculate_route_score(route, places_data)
    
    # Sort routes by score (higher is better)
    routes.sort(key=lambda x: x["score"], reverse=True)

    # Prepare response with at least two routes
    response_data = []
    for route in routes[:2]:  # Take top 2 routes
        start_point = route['legs'][0]['points'][0]
        end_point = route['legs'][0]['points'][-1]
        
        safety_tag = "Safe" if route["police_stations"] > 0 or route["hospitals"] > 0 else "Unsafe"
        traffic_tag = "Fast" if route["summary"]["travelTimeInSeconds"] < 1800 else "Slow"
        
        route_data = {
            "route_index": routes.index(route),
            "distance": route["summary"]["lengthInMeters"],
            "duration": route["summary"]["travelTimeInSeconds"],
            "safety": safety_tag,
            "traffic_info": traffic_tag,
            "police_stations": route["police_stations"],
            "hospitals": route["hospitals"],
            "directions": route["legs"],
            "start_point": start_point,
            "end_point": end_point
        }
        response_data.append(route_data)

    return {
        'statusCode': 200,
        'body': json.dumps(response_data)
    }
