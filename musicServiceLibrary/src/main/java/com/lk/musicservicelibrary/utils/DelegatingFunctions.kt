package com.lk.musicservicelibrary.utils

/**
 * Erstellt von Lena am 07.09.18.
 */
class DelegatingFunctions<ParameterType> {

    private var functions = mutableListOf<listenerFunction<ParameterType>>()

    operator fun plusAssign(function: listenerFunction<ParameterType>) {
        functions.add(function)
    }

    operator fun minusAssign(function: listenerFunction<ParameterType>){
        functions.remove(function)
    }

    fun callWithParameter(parameter: ParameterType){
        for(function in functions){
            function(parameter)
        }
    }

}