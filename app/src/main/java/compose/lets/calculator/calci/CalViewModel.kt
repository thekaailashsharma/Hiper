package compose.lets.calculator.calci

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import compose.lets.calculator.*
import compose.lets.calculator.database.DatabaseRepo
import compose.lets.calculator.database.LiveHistory
import compose.lets.calculator.database.database
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class CalViewModel
@Inject constructor(application: Application) : AndroidViewModel(application) {

    val completeHistory: Flow<List<LiveHistory>>
    private val repository: DatabaseRepo
    val searchResults: MutableLiveData<List<LiveHistory>>

    init {

        val DB = database.getInstance(application)
        val dataDao = DB.hisDao()
        repository = DatabaseRepo(dataDao)
        completeHistory = repository.completeHistory
        searchResults = repository.searchResults
    }

    var state by mutableStateOf(State())
        private set
    var liveState by mutableStateOf(LiveState())
    var itemCount by mutableStateOf(Item())
    var history by mutableStateOf(History())
    var myArray by mutableStateOf(HisArray())

    fun myAction(action: Action) {
        when (action) {
            is Action.Number -> enterNum(action.number)
            is Action.Decimal -> decimal()
            is Action.Clear -> delete()
            is Action.AC -> AllClear()
            is Action.Calculate -> Answer()
            is Action.Operation -> operation(action.operation)
        }
    }

    private fun AllClear() {
        state = State()
        liveState = LiveState()
    }

    private fun operation(oper: Operation) {
        if (state.number1.isNotBlank()) {
            state = state.copy(operation = oper)
        }
    }
    private fun delete() {
        when {
            state.number2.isNotBlank() -> state = state.copy(number2 = state.number2.dropLast(1))
            state.operation != null -> state = state.copy(operation = null)
            state.number1.isNotBlank() -> state = state.copy(number1 = state.number1.dropLast(1))
        }
    }

    private fun decimal() {
        if (state.operation == null && !state
            .number1.contains(".") && !state.number2.contains(".")
        ) {
            state = state.copy(number1 = state.number1 + ".")
        }
        if (!state.number1.contains(".") && !state.number2.contains(".")) {
            state = state.copy(number2 = state.number2 + ".")
        }
    }

    private fun enterNum(number: Int) {
        if (state.operation == null) {
            if (state.number1.length >= 8) {
                return
            }
            state = if (state.number1.toDoubleOrNull() == 0.0) {
                state.copy(number1 = number.toString())
            } else {
                state.copy(number1 = state.number1 + number)
            }
            return
        }
        if (state.number2.length >= 8) {
            return
        }
        state = state.copy(number2 = state.number2 + number)
    }

    private fun Answer() {
        val number1 = state.number1.toDoubleOrNull()
        val number2 = state.number2.toDoubleOrNull()
        var curOp = ""
        if (number2 != null && number1 != null) {
            val result = when (state.operation) {
                Operation.Add -> {
                    curOp = " + "
                    number1 + number2
                }
                Operation.Divide -> {
                    curOp = " / "
                    number1 / number2
                }
                Operation.Modulus -> {
                    curOp = " % "
                    number1 % number2
                }
                Operation.Multiply -> {
                    curOp = " * "
                    number1 * number2
                }
                Operation.Subtract -> {
                    curOp = " - "
                    number1 - number2
                }
                null -> return
            }
            myArray = myArray.copy(hisArray = "$number1 $curOp $number2")

            state = state.copy(
                number1 = result.toString().take(15),
                number2 = "",
                operation = null
            )

            liveState = liveState.copy(answer = result.toString().take(15))
            itemCount = itemCount.copy(count = itemCount.count + 1)
            history = history.copy(history = result.toString())
            viewModelScope.launch {
                repository.insertHistory(
                    LiveHistory(
                        operation = myArray
                            .hisArray,
                        result = history.history
                    )
                )
            }
        }
    }

    fun deleteHistory() {
        viewModelScope.launch {
            repository.deleteHistory()
        }
    }
}
