package compose.lets.calculator.curren.reposirtory

import compose.lets.calculator.curren.api
import compose.lets.calculator.curren.data.dattares
import retrofit2.Response
import javax.inject.Inject

class RemoteDataSource @Inject constructor(private val currencyRatesApi: api) {

    suspend fun getExchangeRates(to:String, from:String, amount: String): Response<dattares>{
        return currencyRatesApi.getRates(to,from,amount)
    }
}