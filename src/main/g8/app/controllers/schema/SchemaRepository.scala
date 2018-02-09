package controllers.schema

import akka.NotUsed
import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.scaladsl.Source
import play.api.libs.json.JsValue

/**
  * Created by Riccardo Merolla on 16/06/17.
  */

object SchemaRepository {

  type SchemaCode = String
  type SchemaForm = JsValue

  sealed trait SchemaEvent

  final case class SetSchema(schemaCode: SchemaCode, schemaForm: SchemaForm)
  final case class RemoveSchema(schemaCode: SchemaCode)
  final case class GetSchema(schemaCode: SchemaCode)

  final case class SchemaFormSet(schemaCode: SchemaCode, schemaForm: SchemaForm) extends SchemaEvent
  final case class SchemaFormRemoved(schemaCode: SchemaCode, schemaForm: SchemaForm) extends SchemaEvent

  final case class GetSchemaEvents(fromSeqNo: Long)
  final case class SchemaEvents(schemaEvents: Source[(Long, SchemaEvent), NotUsed])

  final val NAME = "schema-repository"

  def props(readJournal: EventsByPersistenceIdQuery): Props =
    Props(new SchemaRepository(readJournal))
}

final class SchemaRepository(readJournal: EventsByPersistenceIdQuery)
  extends PersistentActor with ActorLogging {
  import SchemaRepository._

  override val persistenceId = NAME

  private var schemas = Map.empty[SchemaCode, SchemaRepository.SchemaForm]

  override def receiveCommand = {
    case GetSchema(sc) => sender() ! schemas.get(sc)
    case SetSchema(sc, sf) => handleSetSchema(sc, sf)
  }

  override def receiveRecover = {
    case SchemaFormSet(sc, sf) => schemas += sc -> sf
    case SchemaFormRemoved(sc, _) => schemas -= sc
  }

  private def handleSetSchema(schemaCode: SchemaCode, schemaForm: SchemaRepository.SchemaForm) =
    persist(SchemaFormSet(schemaCode, schemaForm)) {
      schemaFormSet =>
        receiveRecover(schemaFormSet)
        log.debug(s"Set SchemaForm with code ${schemaCode}")
        sender() ! schemaFormSet
    }

}
