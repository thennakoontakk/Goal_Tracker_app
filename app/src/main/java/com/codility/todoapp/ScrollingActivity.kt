package com.codility.todoapp

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.codility.recyclerview.MyAdapter
import com.codility.todoapp.databinding.ActivityScrollingBinding
import com.codility.todoapp.helper.DBHelper
import com.codility.todoapp.model.Todo
import java.util.*

class ScrollingActivity : AppCompatActivity(), MyAdapter.OnClickListener {

    private lateinit var binding: ActivityScrollingBinding
    private lateinit var dbHelper: DBHelper
    private var todoList = ArrayList<Todo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrollingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        dbHelper = DBHelper(this)

        binding.fab.setOnClickListener {
            showNoteDialog(false, null, -1)
        }

        // Set the TodoList in myAdapter
        getTodoList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized && binding.list.adapter != null) {
            getTodoList()
        }
    }

    private fun getTodoList() {
        todoList = dbHelper.allNotes
        binding.list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val myAdapter = MyAdapter(todoList)
        myAdapter.setListener(this)
        binding.list.adapter = myAdapter
    }

    private fun deleteConfirmation(todo: Todo) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Confirm Delete...")
        alertDialog.setMessage("Are you sure you want to delete this?")
        alertDialog.setIcon(R.drawable.ic_delete)
        alertDialog.setPositiveButton("YES") { dialog, which ->
            dbHelper.deleteTodo(todo)
            getTodoList()  // Refreshing the list
        }

        alertDialog.setNegativeButton("NO") { dialog, which ->
            dialog.cancel() // Cancel the dialog
        }
        alertDialog.show()
    }

    private fun showNoteDialog(shouldUpdate: Boolean, todo: Todo?, position: Int) {
        val view = LayoutInflater.from(applicationContext).inflate(R.layout.add_todo, null)
        val alertDialogView = AlertDialog.Builder(this).create()
        alertDialogView.setView(view)

        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)
        val edTitle = view.findViewById<EditText>(R.id.edTitle)
        val edDesc = view.findViewById<EditText>(R.id.edDesc)
        val btAddUpdate = view.findViewById<Button>(R.id.btAddUpdate)
        val btCancel = view.findViewById<Button>(R.id.btCancel)

        if (shouldUpdate) btAddUpdate.text = "Update" else btAddUpdate.text = "Save"

        if (shouldUpdate && todo != null) {
            edTitle.setText(todo.title)
            edDesc.setText(todo.desc)
        }

        btAddUpdate.setOnClickListener {
            val tName = edTitle.text.toString()
            val descName = edDesc.text.toString()

            if (TextUtils.isEmpty(tName)) {
                Toast.makeText(this, "Enter Your Title!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (TextUtils.isEmpty(descName)) {
                Toast.makeText(this, "Enter Your Description!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (shouldUpdate && todo != null) {
                updateNote(Todo(tName, descName), position)      // Update note by its id
            } else {
                createNote(Todo(tName, descName))   // Create new note
            }
            alertDialogView.dismiss()
        }

        btCancel.setOnClickListener {
            alertDialogView.dismiss()
        }
        tvHeader.text = if (!shouldUpdate) getString(R.string.lbl_new_todo_title) else getString(R.string.lbl_edit_todo_title)

        alertDialogView.setCancelable(false)
        alertDialogView.show()
    }

    private fun createNote(todo: Todo) {
        val id = dbHelper.insertTodo(todo)
        val new = dbHelper.getTodo(id)
        if (new != null) {
            (binding.list.adapter as? MyAdapter)?.apply {
                todoList.add(0, new)
                notifyDataSetChanged()
            }
        }
    }

    private fun updateNote(todo: Todo, position: Int) {
        val todoList = (binding.list.adapter as? MyAdapter)?.todoList
        todoList?.let {
            val oldTodo = it[position]
            oldTodo.title = todo.title
            oldTodo.desc = todo.desc
            dbHelper.updateTodo(oldTodo)
            it[position] = oldTodo
            (binding.list.adapter as? MyAdapter)?.notifyItemChanged(position)
        }
    }

    override fun onItemDelete(todo: Todo) {
        deleteConfirmation(todo)
    }

    override fun onItemClick(todo: Todo, position: Int) {
        showNoteDialog(true, todo, position)
    }
}
