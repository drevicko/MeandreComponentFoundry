package org.monkproject.utils.math.rootfinders;

/**
 * Interface for testing for convergence in root finders.
 */

public interface RootFinderConvergenceTest {
	/**
	 * Interface for testing for convergence in root finders.
	 * 
	 * @param xNow
	 *            Current root estimate.
	 * @param xPrev
	 *            Previous root estimate.
	 * @param fxNow
	 *            Function value at xNow.
	 * @param xTolerance
	 *            Convergence tolerance for estimates.
	 * @param fxTolerance
	 *            Convergence tolerance for function values.
	 * 
	 * @return true if convergence achieved.
	 */

	public boolean converged(double xNow, double xPrev, double fxNow,
			double xTolerance, double fxTolerance);
}

/*
 * <p> Copyright &copy; 2006-2008 Northwestern University. </p> <p> This program
 * is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version. </p>
 * <p> This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. </p> <p> You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. </p>
 */

