package com.socrata.soda2

import com.rojoma.json.ast.JString

class UnknownTypeException(val name: String) extends SodaProtocolException("Unknown SODA column type " + JString(name))
