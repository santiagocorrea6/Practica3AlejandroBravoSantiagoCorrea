package com.proyect.tradersroom.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.proyect.tradersroom.R
import com.proyect.tradersroom.model.remote.BitacoraRemote
import com.proyect.tradersroom.model.remote.UsuarioRemote
import kotlinx.android.synthetic.main.activity_registro.*
import kotlinx.android.synthetic.main.fragment_bitacora.*
import java.text.SimpleDateFormat
import java.util.*

class BitacoraFragment : Fragment() {

    private lateinit var fecha: String
    private lateinit var id1: String
    private var cal = Calendar.getInstance()
    var maxId = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bitacora, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val format = "MM/dd/yyyy"
                val simpleDateFormat = SimpleDateFormat(format, Locale.US)
                fecha = simpleDateFormat.format(cal.time).toString()
                tv_fecha.text = fecha
            }
        }

        ib_calendario2.setOnClickListener {
            DatePickerDialog(
                requireContext (),
                dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        bt_resumen.setOnClickListener {
            findNavController().navigate(R.id.action_nav_bitacora_to_resumenFragment)
        }

        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

        rula()

        bt_guardar.setOnClickListener {
            val fecha = tv_fecha.text.toString()
            val paridad = sp_divisa.selectedItem.toString()
            val buySell = sp_buySell.selectedItem.toString()
            val inversion = et_inversion.text.toString()
            val rentabilidad = et_rentabilidad.text.toString()
            var resultado = sp_resultado.selectedItem.toString()

            if (inversion.isEmpty()){ //iNVERSION VACIA
                et_inversion.setError("Ingrese su inversion")
            } else if (rentabilidad.isEmpty()){ //RENTABILIDAD VACIA
                et_rentabilidad.setError("Ingrese la rentabilidad")
            } else if (fecha == "MM/dd/yyyy") { //FECHA INCORRECTA
                tv_fecha.error = "Por favor ingrese su fecha de nacimiento"
            } else {

                val correo = consultarCorreo()

                val database = FirebaseDatabase.getInstance()
                val myRef = database.getReference("usuarios")

                val postListener = object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (datasnapshot: DataSnapshot in dataSnapshot.children) {

                            val usuario = datasnapshot.getValue(UsuarioRemote::class.java)

                            if (usuario?.correo == correo) {
                                id1 = "${usuario?.id}"

                                guardarEnBitacora(
                                    database,
                                    resultado,
                                    inversion,
                                    rentabilidad,
                                    fecha,
                                    paridad,
                                    buySell
                                )

                                registroOk()
                            }
                        }
                    }
                }
                myRef.addValueEventListener(postListener)
            }
        }
    }

    private fun guardarEnBitacora(
        database: FirebaseDatabase,
        resultado: String,
        inversion: String,
        rentabilidad: String,
        fecha: String,
        paridad: String,
        buySell: String
    ) {
        val myRef2: DatabaseReference = database.getReference("bitacora").child("$id1")
        val myRef3: DatabaseReference = database.getReference("bitacora").child("$id1")

        val postListener2 = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //if(dataSnapshot.exists()) {
                    maxId = (dataSnapshot.childrenCount.toInt())
                //}
            }
        }

        myRef2.addValueEventListener(postListener2)

        var ganancia: String = ""

        ganancia = calcularGanancia(resultado, inversion, rentabilidad, ganancia)

        val bitacora = BitacoraRemote(
            (maxId+1).toString(),
            "100",
            fecha,
            paridad,
            buySell,
            inversion,
            rentabilidad,
            resultado,
            ganancia
        )

        myRef3.child((maxId+1).toString()).setValue(bitacora)
    }

    private fun consultarCorreo(): String? {
        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val user: FirebaseUser? = mAuth.currentUser
        val correo = user?.email
        return correo
    }


    private fun registroOk() {
        Toast.makeText(requireContext(), "Registro Almacenado", Toast.LENGTH_SHORT).show()
        tv_fecha.setText("")
        et_rentabilidad.setText("")
        et_inversion.setText("")
    }

    private fun calcularGanancia(
        resultado: String,
        inversion: String,
        rentabilidad: String,
        ganancia: String
    ): String {
        var ganancia1 = ganancia
        if (resultado == "Ganada") {
            val a = inversion.toFloat()
            val b = rentabilidad.toFloat() / 100
            ganancia1 = ((a * b).toString())
        } else {
            ganancia1 = (-1 * inversion.toInt()).toString()
        }
        return ganancia1
    }

    private fun rula() {
        val correo = consultarCorreo()

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("usuarios")

        val postListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (datasnapshot: DataSnapshot in dataSnapshot.children) {

                    val usuario = datasnapshot.getValue(UsuarioRemote::class.java)

                    if (usuario?.correo == correo) {
                        id1 = "${usuario?.id}"

                        val myRef2: DatabaseReference =
                            database.getReference("bitacora").child("$id1")
                        val postListener2 = object : ValueEventListener {
                            override fun onCancelled(error: DatabaseError) {
                            }

                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                //if(dataSnapshot.exists()) {
                                maxId = (dataSnapshot.childrenCount.toInt())
                                //}
                            }
                        }

                        myRef2.addValueEventListener(postListener2)
                    }
                }
            }
        }

        myRef.addValueEventListener(postListener)
    }


}