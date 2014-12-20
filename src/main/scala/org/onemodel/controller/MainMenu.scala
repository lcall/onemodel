/*  This file is part of OneModel, a program to manage knowledge.
    Copyright in each year of 2003-2004 and 2008-2015 inclusive, Luke A Call; all rights reserved.}
    (That copyright statement was previously 2013-2015, until I remembered that much of Controller came from TextUI.scala and TextUI.java before that.)
    OneModel is free software, distributed under a license that includes honesty, the Golden Rule, guidelines around binary
    distribution, and the GNU Affero General Public License as published by the Free Software Foundation, either version 3
    of the License, or (at your option) any later version.  See the file LICENSE for details.
    OneModel is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with OneModel.  If not, see <http://www.gnu.org/licenses/>
*/
package org.onemodel.controller

import org.onemodel._
import org.onemodel.model.{IdWrapper, RelationType, EntityClass, Entity}
import org.onemodel.database.PostgreSQLDatabase

class MainMenu(override val ui: TextUI, dbInOVERRIDESmDBWhichHasANewDbConnectionTHATWEDONTWANT: PostgreSQLDatabase) extends Controller(ui) {
  override val mDB = dbInOVERRIDESmDBWhichHasANewDbConnectionTHATWEDONTWANT

  /** See caller in start() for description of the 2nd parameter. */
  // Removed next line @tailrec because 1) it gets errors about "recursive call not in tail position" (which could be fixed by removing the last call to itself,
  // but for the next reason), and 2) it means the user can't press ESC to go "back" to previously viewed entities.
  // ******* IF THIS IS CHANGED BACK, IN THE FUTURE, and we go back to "@tailrec", then we could use a stack of prior entities to for the user to go "back"
  // to, when hitting ESC from the main menu (like the one removed with the same checkin as this writing, but perhaps simplified).
  // @tailrec
  //scoping idea: see idea at beginning of EntityMenu.entityMenu
  def mainMenu(entityIn: Option[Entity] = None, goDirectlyToChoice: Option[Int] = None) {
    try {
      val numEntities = mDB.getEntitiesOnlyCount()
      if (numEntities == 0 || entityIn == None) {
        val choices: List[String] = List[String]("Add new entity (such as yourself using your name, to start)",
                                                 "Search / list existing entities (except quantity units, attribute types, & relation types)")
        val response: Option[Int] = ui.askWhich(None, choices.toArray, Array[String](), includeEscChoice = false, trailingText = Some("^C to quit"))
        if (response != None && response.get != 0) {
          val answer = response.get
          // None means user hit ESC (or 0, though not shown) to get out
          answer match {
            case 1 => showInEntityMenuThenMainMenu(askForInfoAndCreateEntity())
            case 2 =>
              val selection: Option[IdWrapper] = chooseOrCreateObject(None, None, None, Controller.ENTITY_TYPE)
              if (selection != None) {
                showInEntityMenuThenMainMenu(Some(new Entity(mDB, selection.get.getId)))
              }
            case _ => ui.displayText("unexpected: " + answer)
          }
        }
      } else if (Entity.getEntityById(mDB, entityIn.get.getId) == None) {
        ui.displayText("The entity to be displayed, id " + entityIn.get.getId + ": " + entityIn.get.getDisplayString + "\", is not present, " +
                       "probably because it was deleted.  Trying the prior one viewed.", waitForKeystroke = false)
        // then allow exit from this method so the caller will thus back up one entity and re-enter this menu.
      } else {
        require(entityIn != None)
        // We have an entity, so now we can act on it:

        // First, get a fresh copy in case things changed since the one passed in as the parameter was read, like edits etc since it was last saved by,
        // or passed from the calling menuLoop (by this or another process):
        val entity: Entity = new Entity(mDB, entityIn.get.getId)

        val leadingText: String = "Main OM menu:"
        val choices: List[String] = List[String](menuText_createEntityOrAttrType,
                                                 menuText_CreateRelationType,
                                                 "----" /*spacer for better consistency of options with other menus, for memory & navigation speed*/ ,
                                                 "----" /*spacer for better consistency of options with other menus, for memory & navigation speed*/ ,
                                                 "Go to current entity (" + entity.getDisplayString + "; or its sole subgroup, if present)",
                                                 "Search / list existing entities (except quantity units, attr types, & relation types)",
                                                 "List existing classes",
                                                 "List existing relation types")
        val response =
          if (goDirectlyToChoice == None) ui.askWhich(Some(Array(leadingText)), choices.toArray, Array[String](), includeEscChoice = true,
                                                      trailingText = Some("^C to quit (anytime)"))
          else goDirectlyToChoice

        if (response != None && response.get != 0) {
          val answer = response.get
          answer match {
            case 1 =>
              showInEntityMenuThenMainMenu(askForInfoAndCreateEntity())
            case 2 =>
              showInEntityMenuThenMainMenu(askForNameAndWriteEntity(Controller.RELATION_TYPE_TYPE))
            case 5 =>
              val subEntitySelected: Option[Entity] = goToEntityOrItsSoleGroupsMenu(entity)._1
              if (subEntitySelected != None) mainMenu(subEntitySelected)
            case 6 =>
              val selection: Option[IdWrapper] = chooseOrCreateObject(None, None, None, Controller.ENTITY_TYPE)
              if (selection != None) {
                showInEntityMenuThenMainMenu(Some(new Entity(mDB, selection.get.getId)))
              }
            case 7 =>
              val classId: Option[IdWrapper] = chooseOrCreateObject(None, None, None, Controller.ENTITY_CLASS_TYPE)
              // (compare this to showInEntityMenuThenMainMenu)
              if (classId != None) {
                new ClassMenu(ui, mDB).classMenu(new EntityClass(mDB, classId.get.getId))
                mainMenu(Some(entity))
              }
            case 8 =>
              val rtId: Option[IdWrapper] = chooseOrCreateObject(None, None, None, Controller.RELATION_TYPE_TYPE)
              if (rtId != None) {
                showInEntityMenuThenMainMenu(Some(new RelationType(mDB, rtId.get.getId)))
              }
            case _: Int =>
              ui.displayText("unexpected: " + answer)
          }
          // Show main menu here, in case user hit ESC from an entityMenu (which returns None): so they'll still see the entity they expect next.
          mainMenu(Some(entity))
        }
      }
    }
    catch {
      case e: Exception =>
        showException(e)
        val ans = ui.askYesNoQuestion("Go back to what you were doing (vs. going out)?",Some("y"))
        if (ans != None && ans.get) mainMenu(entityIn, goDirectlyToChoice)
    }
  }

}
