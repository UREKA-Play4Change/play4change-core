package com.ureka.play4change.result.throwable

import com.ureka.play4change.error.AppError

class ResultControlFlowException(val error: AppError) : RuntimeException()