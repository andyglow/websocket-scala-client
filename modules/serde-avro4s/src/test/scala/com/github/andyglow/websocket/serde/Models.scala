package com.github.andyglow.websocket.serde

import com.sksamuel.avro4s.Decoder
import com.sksamuel.avro4s.Encoder
import com.sksamuel.avro4s.SchemaFor

object Models {

  case class NestedTestModel[T](
    id: String,
    value: T,
    active: Boolean
  )

  case class NestedEntry(
    id: String,
    value: Double
  )

  case class TestModel(
    id: String,
    count: Int,
    series: List[Double],
    nested: NestedTestModel[Long],
    seriesOfNested: List[NestedEntry]
  )
}
