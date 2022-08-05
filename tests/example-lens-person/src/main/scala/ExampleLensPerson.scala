import java.time.LocalDate

import monocle.Lens
import monocle.syntax.all._
import monocle.macros.syntax.lens._
import monocle.macros.GenLens

object ExampleLensPerson {
  case class Person(_name: Name, _born: Born, _address: Address)

  case class Name(_foreNames: String /*Space separated*/ , _surName: String)

  // Value of java.time.LocalDate.toEpochDay
  type EpochDay = Long

  case class Born(_bornAt: Address, _bornOn: EpochDay)

  case class Address(_street: String, _houseNumber: Int,
    _place: String /*Village / city*/ , _country: String)

  // Valid values of Gregorian are those for which 'java.time.LocalDate.of'
  // returns a valid LocalDate.
  case class Gregorian(_year: Int, _month: Int, _dayOfMonth: Int)

  // Lenses
  val personAddressStreet = GenLens[Person](_._address._street)

  val personBirthStreet = GenLens[Person](_._born._bornAt._street)

  val personBirthdayMonth = Lens[Person, Int]
    {person =>
      LocalDate.ofEpochDay(person._born._bornOn).getMonth().getValue()
    }
    {birthMonth => person =>
      val newBirth = LocalDate.ofEpochDay(person._born._bornOn).withMonth(birthMonth).toEpochDay()
      person.copy(_born = person._born.copy(_bornOn = newBirth))
    }

  // to implement
  val bornStreet: Born => String = _._bornAt._street

  val setCurrentStreet: String => Person => Person = street => person =>
    personAddressStreet.set(street)(person)

  val setBirthMonth: Int => Person => Person = birthMonth => person => 
    personBirthdayMonth.set(birthMonth)(person)

  // Transform both birth and current street names.
  val renameStreets: (String => String) => Person => Person = streetModifier => person =>
    (personAddressStreet.modify(streetModifier) compose personBirthStreet.modify(streetModifier))(person)    
}
