package com.mnvpatni.rakhwala.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mnvpatni.rakhwala.R
import com.mnvpatni.rakhwala.Route

class RoutesAdapter(private val routes: List<Route>, private val onRouteClick: (Route) -> Unit) : RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        holder.bind(route)
    }

    override fun getItemCount(): Int {
        return routes.size
    }

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val distanceText: TextView = itemView.findViewById(R.id.distance)
        private val durationText: TextView = itemView.findViewById(R.id.duration)
        private val badge: TextView = itemView.findViewById(R.id.routeBadge)

        fun bind(route: Route) {
            // Convert distance to km and duration to minutes or hours
            val distanceInKm = route.distance / 1000.0
            val durationInMinutes = route.duration / 60
            val durationInHours = durationInMinutes / 60

            distanceText.text = String.format("%.2f km", distanceInKm)

            if (durationInHours >= 1) {
                durationText.text = String.format("%d hours %d mins", durationInHours.toInt(), durationInMinutes % 60)
            } else {
                durationText.text = String.format("%d mins", durationInMinutes)
            }

            // Set the badge based on the route safety status
            when (route.safety) {
                "Safe" -> badge.apply {
                    text = route.safety
                    setBackgroundColor(itemView.context.getColor(R.color.green))  // Replace with actual color from resources
                }
                "Not Safe" -> badge.apply {
                    text = route.safety
                    setBackgroundColor(itemView.context.getColor(R.color.red))  // Replace with actual color from resources
                }
                else -> badge.apply {
                    text = route.safety
                    setBackgroundColor(itemView.context.getColor(R.color.yellow))  // Replace with actual color from resources
                }
            }

            // Set click listener to open Google Maps
            itemView.setOnClickListener { onRouteClick(route) }
        }
    }
}
