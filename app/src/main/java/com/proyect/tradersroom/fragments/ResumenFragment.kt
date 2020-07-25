package com.proyect.tradersroom.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.proyect.tradersroom.R
import com.proyect.tradersroom.model.remote.BitacoraRemote
import com.proyect.tradersroom.model.remote.UsuarioRemote
import com.proyect.tradersroom.ui.BitacorasRVAdapter
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_perfil.*
import kotlinx.android.synthetic.main.fragment_resumen.*

class ResumenFragment : Fragment() {

    private val bitacorasList: MutableList<BitacoraRemote> = mutableListOf()
    private lateinit var bitacorasAdapter : BitacorasRVAdapter
    var bitacoraId = "hola"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resumen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tv_capitalActual.text = "$10"
        tv_capitalInicial.text = "$10"

        cargarBitacora()

        //Toast.makeText(requireContext(), "Actual: $capitalInicial", Toast.LENGTH_SHORT).show()

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
        val correo = consultarUsuario()

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("usuarios")



        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(datasnapshot: DataSnapshot in dataSnapshot.children) {

                    val usuario = datasnapshot.getValue(UsuarioRemote::class.java)

                    if (usuario?.correo == correo) {

                        val myRef2 = database.getReference("bitacora").child("${usuario?.id}")

                        val postListener = object : ValueEventListener {
                            override fun onCancelled(error: DatabaseError) {
                            }

                            override fun onDataChange(snapshot: DataSnapshot) {
                                var capitalActual : String = ""
                                var capitalInicial : String = ""

                                for(datasnapshot: DataSnapshot in snapshot.children){
                                    val bitacora = datasnapshot.getValue(BitacoraRemote::class.java)
                                    bitacorasList.add(bitacora!!)

                                    capitalActual = bitacora.capitalInicial
                                    //tv_capitalActual.text = "$$capitalActual"
                                    //tv_capitalInicial.text = "$$capitalInicial"
                                    if (bitacora.id == "0") {
                                        capitalInicial = bitacora.capitalInicial
                                        //tv_capitalInicial.text = "$$capitalInicial"
                                    }

                                    writeInTextView(capitalActual, capitalInicial)
                                }
                                bitacorasAdapter.notifyDataSetChanged()
                            }
                        }
                        myRef2.addValueEventListener(postListener)
                    }

                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error al cargar", Toast.LENGTH_SHORT).show()
            }
        }
        myRef.addValueEventListener(postListener)
    }

    private fun writeInTextView(capitalActual: String, capitalInicial: String) {
        tv_capitalInicial.setText("$$capitalInicial")
        tv_capitalActual.setText("$$capitalActual")
    }


    private fun consultarUsuario(): String? {
        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val user: FirebaseUser? = mAuth.currentUser
        val correo = user?.email
        return correo
    }

}