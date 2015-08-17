/*  This file is part of OneModel, a program to manage knowledge.
    Copyright in each year of 2014-2015 inclusive, Luke A Call; all rights reserved.
    OneModel is free software, distributed under a license that includes honesty, the Golden Rule, guidelines around binary
    distribution, and the GNU Affero General Public License as published by the Free Software Foundation, either version 3
    of the License, or (at your option) any later version.  See the file LICENSE for details.
    OneModel is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with OneModel.  If not, see <http://www.gnu.org/licenses/>
*/
package org.onemodel

import java.io.File
import java.nio.file.{Files, Path}

import org.onemodel.controller.{ImportExport, Controller}
import org.onemodel.database.PostgreSQLDatabase
import org.onemodel.model.Entity
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Args, FlatSpec, Status}

import scala.collection.mutable

/**
 * IT IS IMPORTANT THAT SUCH TESTS USE THE SAME DB variable INSIDE ANY ELEMENTS PASSED IN TO THE TEST METHOD, AS IN THE CONTROLLER SUBCLASS THAT IS BEING
 * TESTED!!, OTHERWISE YOU GET TWO DB CONNECTIONS, AND THEY CAN'T SEE EACH OTHERS' DATA BECAUSE IT'S INSIDE A TRANSACTION, AND IT'S A MESS.
 * Idea: Think more about cleaner design, for this?  Like, how to avoid the problems I had when accidentally just using mImportExport.mDB during tests,
 * which by default connects to the user db, not the test db!?
 */
class ImportExportTest extends FlatSpec with MockitoSugar {
  var mEntity: Entity = null

  // idea!!: instead of "new TextUI" pass in something that extends or implements a parent of them both, and does the right things for tests (like, maybe
  // answers everything in a particular way?):  but it shouldn't be used at all, anyway in this case).  When that is done, one can remove the "testing = true"
  // parameter below.
  val ui = new TextUI
  var mImportExport: ImportExport = null
  var mDB: PostgreSQLDatabase = null

  override def runTests(testName: Option[String], args: Args): Status = {
    setUp()
    val result: Status = super.runTests(testName, args)
    // (not calling tearDown: see comment inside PostgreSQLDatabaseTest.runTests about "db setup/teardown")
    result
  }

  protected def setUp() {
    //start fresh
    PostgreSQLDatabaseTest.tearDownTestDB()

    //// instantiation does DB setup (creates tables, default data, etc):
    mDB = new PostgreSQLDatabase("testrunner", "testrunner")
    mDB.createRelationType("a test relation type","","UNI")
    // a bad smell: shouldn't need a ui (& maybe not a controller?) to run tests of logic.  Noted in tasks to fix.
    mImportExport = new ImportExport(ui, mDB, new Controller(ui))

    val entityId: Long = mDB.createEntity("test object")
    mEntity = new Entity(mDB, entityId)
  }

  protected def tearDown() {
    PostgreSQLDatabaseTest.tearDownTestDB()
  }

  private def tryImporting(filename: String) {
    val stream = this.getClass.getClassLoader.getResourceAsStream(filename)
    val reader: java.io.Reader = new java.io.InputStreamReader(stream)
    // manual testing alternative to the above 2 lines, such as for use w/ interactive scala (REPL):
    //val path = "PUT-Full-path-to-some-text-file-here"
    //val fileToImport = new File(path)
    //val reader = new FileReader(fileToImport)

    mImportExport.doTheImport(reader, "name", 0L, mEntity, creatingNewStartingGroupFromTheFilenameIn = false, addingToExistingGroup = false,
                              putEntriesAtEnd = true, mixedClassesAllowedDefaultIn = true, testing = true, makeThemPublicIn = Some(false))
  }

  def tryExporting(ids: Option[List[Long]]): (String, Array[String]) = {
    assert(ids.get.nonEmpty)
    val entityId: Long = ids.get.head
    val startingEntity: Entity = new Entity(mDB, entityId)

    val exportedEntities = new mutable.TreeSet[Long]()
    val prefix: String = mImportExport.getExportFileNamePrefix(startingEntity, ImportExport.HTML_EXPORT_TYPE)
    val outputDirectory: Path = mImportExport.createOutputDir("omtest-" + prefix)
    val uriClassId: Option[Long] = mDB.findFIRSTClassIdByName("URI", caseSensitive = true)
    mImportExport.exportHtml(startingEntity, levelsToExportIsInfinite = true, 0, outputDirectory, exportedEntities, mutable.TreeSet[Long](), uriClassId,
                                    Some(true), Some(true), Some(true), Some("2015 thisisatestpersonname"))

    assert(outputDirectory.toFile.exists)
    val newFiles: Array[String] = outputDirectory.toFile.list
    val firstNewFileName = "e" + entityId + ".html"
    val firstNewFile = new File(outputDirectory.toFile, firstNewFileName)
    val firstNewFileContents: String = new Predef.String(Files.readAllBytes(firstNewFile.toPath))
    assert(newFiles.contains(firstNewFileName), "unexpected filenames, like: " + newFiles(0))
    (firstNewFileContents, newFiles)
  }

  "testImportBasic" should "work without throwing an Exception" in {
    val name = "testImportBasic"
    System.out.println("starting " + name)
    tryImporting("testImportFile1.txt")
  }

  "testImportBadTaFormat1" should "demonstrate throwing an exception" in {
    val name = "testImportBadTaFormat1"
    System.out.println("starting " + name)
    intercept[OmException] {
                             tryImporting("testImportFile2.txt")
                           }
  }

  "testImportBadTaFormat2" should "also demonstrate throwing an exception" in {
    val name = "testImportBadTaFormat2"
    System.out.println("starting " + name)
    intercept[OmException] {
                             tryImporting("testImportFile3.txt")
                           }
  }

  "testImportGoodTaFormat" should "demonstrate importing with content to become a TextAttribute, specifying a valid attribute type name" in {
    val name = "testImportGoodTaFormat"
    System.out.println("starting " + name)

    // no exceptions:
    tryImporting("testImportFile4.txt")

    // make sure it actually imported something expected:
    val ids: Option[List[Long]] = mDB.findAllEntityIdsByName("lastTopLevelLineIn-testImportFile4.txt")
    assert(ids.get.nonEmpty)
    var foundIt = false
    val relationTypeId = mDB.findRelationType(PostgreSQLDatabase.theHASrelationTypeName)._1
    for (id <- ids.get) {
      // (could have used mDB.getContainingEntities1 here perhaps)
      if (mDB.relationToEntityKeyExists(relationTypeId.get, mEntity.getId, id)) {
        foundIt = true
      }
    }
    assert(foundIt)
  }

  "testExportHtml" should "work" in {
    System.out.println("starting testExportHtml")

    tryImporting("testImportFile4.txt")
    val ids: Option[List[Long]] = mDB.findAllEntityIdsByName("vsgeer4")
    val (firstNewFileContents: String, newFiles: Array[String]) = tryExporting(ids)

    assert(firstNewFileContents.contains("<a href=\"e-"), "unexpected file contents:  " + firstNewFileContents)
    assert(firstNewFileContents.contains(".html\">purpose</a> (0)"), "unexpected file contents:  " + firstNewFileContents)
    assert(firstNewFileContents.contains(".html\">empowerment</a> (2)"), "unexpected file contents:  " + firstNewFileContents)
    assert(firstNewFileContents.contains("Copyright"), "unexpected file contents: no copyright?")
    assert(firstNewFileContents.contains("all rights reserved"), "unexpected file contents: no 'all rights reserved'?")
    assert(newFiles.length > 5, "unexpected # of files: " + newFiles.length)
  }

  "testImportAndExportOfUri" should "work" in {
    System.out.println("starting testImportAndExportOfUri")

    tryImporting("testImportFile5.txt")
    val ids: Option[List[Long]] = mDB.findAllEntityIdsByName("import-file-5")
    val firstNewFileContents: String = tryExporting(ids)._1
    assert(firstNewFileContents.contains("<a href=\"http://www.onemodel.org/downloads/testfile.txt\">test file download</a>"), "unexpected file contents:  " + firstNewFileContents)
  }

}
