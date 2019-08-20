package lutrom

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.iotesters.PeekPokeTester
import chisel3._

class FPGreaterThanTests(c: FPGreaterThan) extends PeekPokeTester(c) {
  
  // negative < positive (-1.5 < 1.5)
  poke(c.io.greater, 0xBFC00000)
  poke(c.io.lesser, 0x3FC00000)
  step(1)
  expect(c.io.greater_than, 0)

  // positive > negative (1.5 > -1.5)
  poke(c.io.greater, 0x3FC00000)
  poke(c.io.lesser, 0xBFC00000)
  step(1)
  expect(c.io.greater_than, 1)

  // negative < negative (-1.75 < -1.5)
  poke(c.io.greater, 0xBFE00000)
  poke(c.io.lesser, 0xBFC00000)
  step(1)
  expect(c.io.greater_than, 0)

  // negative > negative (-1.5 > -1.75)
  poke(c.io.greater, 0xBFC00000)
  poke(c.io.lesser, 0xBFE00000)
  step(1)
  expect(c.io.greater_than, 1)

  // positive > positive (1.75 > 1.5)
  poke(c.io.greater, 0x3FE00000)
  poke(c.io.lesser, 0x3FC00000)
  step(1)
  expect(c.io.greater_than, 1)

  // positive > positive (1.5 < 1.75)
  poke(c.io.greater, 0x3FC00000)
  poke(c.io.lesser, 0x3FE00000)
  step(1)
  expect(c.io.greater_than, 0)
}

