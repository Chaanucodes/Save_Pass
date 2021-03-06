package com.pilot

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.pilot.database.FillsDTO
import com.pilot.database.FillsDatabase


class MyAutoFillService : AutofillService(){

    private val emailFields: MutableList<ViewNode?> = ArrayList()
    private val passwordFields: MutableList<ViewNode?> = ArrayList()
    private var currentEmail = ""
    private var currentPassword = ""


    override fun onFillRequest(p0: FillRequest, p1: CancellationSignal, p2: FillCallback) {
        val context: List<FillContext> = p0.fillContexts
        val structure: AssistStructure = context[context.size - 1].structure

        //getting values from the database
        val dataSource = FillsDatabase.getInstance(applicationContext).fillsDatabaseDao
        var fillsList  = dataSource.getAllFills().value
        print("THIS THANG : ${fillsList?.size}")


        //Choosing a default password initially if there is some entry in the database
            if (!fillsList.isNullOrEmpty()){
                currentEmail = fillsList[0].username
                currentPassword = fillsList[0].password
            }


        //To check how may times the service is started
        println("MY AUTOCALLED")

        //Looking for email and password fields
        identifyEmailFields(
            structure
                .getWindowNodeAt(0)
                .rootViewNode, emailFields, fillsList
        )

        identifyPasswordFields(
            structure
                .getWindowNodeAt(0)
                .rootViewNode, passwordFields, fillsList
        )


        if(emailFields.size == 0 && passwordFields.size == 0)
            return

        //Views for displaying custom suggestion
        val rvPrimaryEmail: RemoteViews = RemoteViews(packageName, com.pilot.R.layout.email_suggestion)
        val rvPrimaryPassword: RemoteViews = RemoteViews(packageName, com.pilot.R.layout.email_suggestion)

        rvPrimaryEmail.setTextViewText(com.pilot.R.id.email_suggestion_item, currentEmail)
        val emailField : ViewNode? = if(emailFields.isEmpty()) null else emailFields[0]

        rvPrimaryPassword.setTextViewText(com.pilot.R.id.email_suggestion_item, "pass for $currentEmail")
        val passwordField : ViewNode? = if(passwordFields.isEmpty()) null else passwordFields[0]

        //Building dataset for email
        var primaryEmailDataSet: Dataset? = null

        emailField?.let {
            primaryEmailDataSet = Dataset.Builder(rvPrimaryEmail)
                .setValue(
                    it.autofillId!!,
                    AutofillValue.forText(currentEmail)
                ).build()
        }


        //Building dataser for password
        var primaryPasswordDataSet: Dataset? = null

        passwordField?.let {
           primaryPasswordDataSet = Dataset.Builder(rvPrimaryPassword)
                .setValue(
                    it.autofillId!!,
                    AutofillValue.forText(currentPassword)
                ).build()
        }

        //setting final response
        val response = FillResponse.Builder().apply {
            primaryEmailDataSet?.let {
                this.addDataset(it)
            }
            primaryPasswordDataSet?.let {
                this.addDataset(it)
            }
        }.build()

        // If there are no errors, call onSuccess() and pass the response
        p2.onSuccess(response)
    }

    //Save feature not yet implemented
    override fun onSaveRequest(p0: SaveRequest, p1: SaveCallback) {
    }

    fun identifyEmailFields(
        node: ViewNode?,
        emailFields: MutableList<ViewNode?>?,
        fillsList: List<FillsDTO>?
    ) {

        //To check what all information of the running app is being received by our service
        println("NODEDATA => ${node?.text}")

        //Checking if we have found the textfield or Edit Text field
        node!!.className?.let {
            if (it.contains("EditText") || it.contains("TextInput")
                || it.contains("textfield")) {
                //Checking if the page contains the domain name we are looking for
                if(fillsList != null && node!= null){
                    fillsList.forEach{
                        val dName = it.webDomain.trim().removePrefix("www.").removeSuffix(".com")
                        node.text?.let {temp ->
                            print("DE DATA: $dName")
                            if(temp.contains(dName)){
                                currentEmail = it.username
                            }
                        }
                    }
                }

                val viewId = node.idEntry
                if (viewId != null && (viewId.contains("mail")
                            || viewId.contains("user") ||
                            viewId.contains("Name"))
                ) {
                    emailFields?.add(node)
                    return
                }else{
                    node!!.hint?.let { str ->
                        if (str.contains("mail")
                            || str.contains("user") ||
                            str.contains("Name")
                        ) {
                            emailFields?.add(node)
                            return
                        }
                }
            }
            }
            //Checking all the nodes(items/views) that are displayed on the screen to look for email fields
            for (i in 0 until node.childCount) {
                identifyEmailFields(node.getChildAt(i), emailFields, fillsList)
            }
        }
    }

    fun identifyPasswordFields(
        node: ViewNode?,
        passwordFields: MutableList<ViewNode?>?,
        fillsList: List<FillsDTO>?
    ) {


        node!!.className?.let {
            if (it.contains("EditText") || it.contains("TextInput")
                || it.contains("textfield")) {
                //Checking if the page contains the domain name we are looking for
                if(fillsList != null && node!= null){
                    fillsList.forEach{
                        val dName = it.webDomain.trim().removePrefix("www.").removeSuffix(".com")
                        node.text?.let {temp ->
                            if(temp.contains(dName)){
                                currentPassword = it.password
                            }
                        }
                    }
                }


                val viewId = node.idEntry
                if (viewId != null && (viewId.contains("assword"))
                ) {
                    passwordFields?.add(node)
                    return
                }else{
                    node!!.hint?.let { str ->
                        if (str.contains("assword")
                        ) {
                            passwordFields?.add(node)
                            return
                        }
                    }
                }
            }
            //Checking all the nodes(items/views) that are displayed on the screen to look for password fields
            for (i in 0 until node.childCount) {
                identifyPasswordFields(node.getChildAt(i), passwordFields, fillsList)
            }
        }
    }

}