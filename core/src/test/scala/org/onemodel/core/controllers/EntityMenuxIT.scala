/*  This file is part of OneModel, a program to manage knowledge.
    Copyright in each year of 2017-2017 inclusive, Luke A. Call; all rights reserved.
    OneModel is free software, distributed under a license that includes honesty, the Golden Rule, guidelines around binary
    distribution, and the GNU Affero General Public License as published by the Free Software Foundation.
    See the file LICENSE for license version and details.
    OneModel is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with OneModel.  If not, see <http://www.gnu.org/licenses/>
*/
package org.onemodel.core.controllers

import java.text.SimpleDateFormat
import java.util.Date

import org.testng.annotations._
import org.testng.Assert._

/** Experimenting to see if scala can be called by the failsafe plugin [can't, IIRC now] as part of the integration tests. Compare to
  * EntityMenuIt.java.
  */
class EntityMenuxIT {
  def setUp(): Unit = {
  }
  def tearDown() {
  }

  def testSomething: Unit = {
    throw new Exception("adf");
  }
}