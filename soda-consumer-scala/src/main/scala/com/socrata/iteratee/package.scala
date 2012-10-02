package com.socrata

import com.rojoma.json.util.WrappedCharArray

package object iteratee {
  type CharIteratee[T] = Iteratee[WrappedCharArray, T]
  type ByteIteratee[T] = Iteratee[Array[Byte], T]
}
