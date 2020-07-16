package com.proyect.tradersroom.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.proyect.tradersroom.R
import com.proyect.tradersroom.model.remote.BitacoraRemote
import com.proyect.tradersroom.ui.BitacorasRVAdapter
import kotlinx.android.synthetic.main.fragment_resumen.*

class ResumenFragment : Fragment() {

    private val bitacorasList: MutableList<BitacoraRemote> = mutableListOf()
    private lateinit var bitacorasAdapter : BitacorasRVAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resumen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarBitacora()

        rv_bitacoras.layoutManager = LinearLayoutManager(
            requireContext(),
            RecyclerView.VERTICAL,
            false
        )

        rv_bitacoras.setHasFixedSize(true) //todos del mismo tamaño

        bitacorasAdapter = BitacorasRVAdapter(bitacorasList as ArrayList<BitacoraRemote>)
        rv_bitacoras.adapter = bitacorasAdapter

    }

    private fun cargarBitacora() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("bitacora").child("santiagocorrea54")


        val postListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for(datasnapshot: DataSnapshot in snapshot.children){
                    val bitacora = datasnapshot.getValue(BitacoraRemote::class.java)
                    bitacorasList.add(bitacora!!)
                }

                bitacorasAdapter.notifyDataSetChanged()
            }
        }
        myRef.addValueEventListener(postListener)
    }
}