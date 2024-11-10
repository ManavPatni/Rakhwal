package com.mnvpatni.rakhwala.adaper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mnvpatni.rakhwala.R
import com.mnvpatni.rakhwala.SosContact

class ContactAdapter(
    private var contacts: List<SosContact>,
    private val onDeleteClick: (SosContact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    // ViewHolder class to hold views and bind the data
    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.contact_name)
        private val phoneTextView: TextView = itemView.findViewById(R.id.contact_phone)

        // Bind contact data to the views
        fun bind(contact: SosContact) {
            nameTextView.text = contact.name
            phoneTextView.text = contact.phoneNumber

            // Long click listener to delete contact
            itemView.setOnLongClickListener {
                onDeleteClick(contact)
                true // Return true to indicate that the long click is handled
            }
        }
    }

    // Inflate the item layout and return the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    // Bind the data to the ViewHolder for the item at the given position
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    // Return the size of the data set (contacts list)
    override fun getItemCount() = contacts.size

    // Update the list of contacts and notify RecyclerView of the change
    fun updateContacts(newContacts: List<SosContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
