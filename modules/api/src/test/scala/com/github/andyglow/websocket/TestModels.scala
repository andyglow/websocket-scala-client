package com.github.andyglow.websocket

object TestModels {

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
