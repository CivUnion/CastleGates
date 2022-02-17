/**
 * @author Aleksey Terzi
 *
 */

package com.aleksey.castlegates;

import org.bukkit.block.Block;

@SuppressWarnings("deprecation")
public class DeprecatedMethods {

	public static byte getMeta(Block block) {
		return block.getData();
	}
}
