/*  This file is part of OneModel, a program to manage knowledge.
    Copyright in each year of 2014-2014 inclusive, Luke A Call; all rights reserved.
    OneModel is free software, distributed under a license that includes honesty, the Golden Rule, guidelines around binary
    distribution, and the GNU Affero General Public License as published by the Free Software Foundation, either version 3
    of the License, or (at your option) any later version.  See the file LICENSE for details.
    OneModel is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with OneModel.  If not, see <http://www.gnu.org/licenses/>

  ---------------------------------------------------
  If we ever do port to another database, create the Database interface (removed around 2014-1-1 give or take) and see other changes at that time.
  An alternative method is to use jdbc escapes (but this actually might be even more work?):  http://jdbc.postgresql.org/documentation/head/escapes.html  .
  Another alternative is a layer like JPA, ibatis, hibernate  etc etc.

*/
package org.onemodel

import org.scalatest.FlatSpec
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.onemodel.model.FileAttribute
import org.onemodel.database.PostgreSQLDatabase

class FileAttributeTest extends FlatSpec with MockitoSugar {
  "getDisplayString" should "return correct string and length" in {
    val mockDB = mock[PostgreSQLDatabase]
    val entityId: Long = 0
    val otherEntityId: Long = 1
    val fileAttributeId: Long = 0
    //arbitrary, in milliseconds:
    val modifiedDate: Long = 304
    val storedDate: Long = modifiedDate + 1
    val attrTypeName = "txt format"
    val longDescription = "this is a longish description of a file"
    val filePath = "/tmp/w.jpeg"
    val size = 12345678
    val hash = "e156b9a37060ccbcbffe5ec0fc967016"
    when(mockDB.getEntityName(otherEntityId)).thenReturn(Some(attrTypeName))
    when(mockDB.fileAttributeKeyExists(fileAttributeId)).thenReturn(true)

    // (using arbitrary numbers for the unnamed parameters):
    val fileAttribute = new FileAttribute(mockDB, fileAttributeId, entityId, otherEntityId, longDescription, modifiedDate, storedDate, filePath, true, true,
                                          false, size, hash)
    val smallLimit = 35
    val display1: String = fileAttribute.getDisplayString(smallLimit)
    val wholeThing: String = longDescription + " (" + attrTypeName + "); 12MB (12345678) rw- from " + filePath + ", " +
                             "mod Wed 1969-12-31 17:00:00:" + modifiedDate + " MST, " +
                             "stored Wed 1969-12-31 17:00:00:" + storedDate + " MST; md5 " +
                             hash + "."
    val expected: String = wholeThing.substring(0, smallLimit - 3) + "..." // put the real string here instead of dup logic?
    assert(display1 == expected)

    val unlimited = 0
    val display2: String = fileAttribute.getDisplayString(unlimited)
    assert(display2 == wholeThing)
  }

  "getReplacementFilename" should "work" in {
    // This all is intended to make the file names made by File.createTempFile come out in a nice (readable/memorable/similar such as for hidden) way when it
    // is used.
    val fileAttributeId = 0L
    val mockDB = mock[PostgreSQLDatabase]
    when(mockDB.fileAttributeKeyExists(fileAttributeId)).thenReturn(true)

    var originalName = ""
    val fa: FileAttribute = new FileAttribute(mockDB, fileAttributeId) {override def getOriginalFilePath: String = originalName}
    val (basename, extension) = FileAttribute.getReplacementFilename(fa.getOriginalFilePath)
    assert(basename == FileAttribute.filenameFiller && extension == "")

    originalName = "something.txt"
    val (basename2, extension2) = FileAttribute.getReplacementFilename(fa.getOriginalFilePath)
    assert(basename2 == "something" && extension2 == ".txt")

    originalName = "somefilename"
    val (basename3, extension3) = FileAttribute.getReplacementFilename(fa.getOriginalFilePath)
    assert(basename3 == "somefilename" && extension3 == "")

    originalName = ".hidden"
    val (basename4, extension4) = FileAttribute.getReplacementFilename(fa.getOriginalFilePath)
    assert(basename4 == ".hidden" && extension4 == "")

    originalName = "1.txt"
    val (basename5, extension5) = FileAttribute.getReplacementFilename(fa.getOriginalFilePath)
    assert(basename5 == "1aa" && extension5 == ".txt")

    originalName = "some.long.thing"
    val (basename6, extension6) = FileAttribute.getReplacementFilename(fa.getOriginalFilePath)
    assert(basename6 == "some.long" && extension6 == ".thing")
  }

}
