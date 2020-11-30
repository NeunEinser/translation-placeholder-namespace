# translation-placeholder-namespace

This is a simple java program to transform a Minecraft namespace into a language file placeholder string.  
This is mostly useful, when you want a json text differently depending on if a resource pack is active or not

For example, to display a different json text component depending on if a certain resource pack is active, one might setup following pack:  
`assets/minecraft/en_us.json`
```json
{
    "%1$s": "%2$s"
}
```
Json text component:
```json
{
	"translate":"%1$s",
	"with": [
		[
			"Could not find required resource pack. ",
			{
				"text": "[Click here to download the resource pack]",
				"clickEvent": {
					"action":"open_url",
					"value": "https://github.com/NeunEinser/translation-placeholder-namespace"
				}
			}
		],
		[
			"You are good to go! ",
			{
				"text": "[Click here to go to the lobby]",
				"clickEvent": {
					"action":"run_command",
					"value": "/say Teleport malefunction. Please contact someone else then me."
				}
			}
		]
	]
}
```
Because the translation key will be interpreted as placeholder #1, the first json text component of the with clause will be chosen, in case the translation key above is not present.  
If it is present, the second one will be used again.

In case multiple packs use the same method, they will not be compatible with each other, though.  
This program will give you a unique string of translation key placeholders, which will be replaced by an empty string when displayed in game, to prevent compatibility issues with other packs.

## Usage
`java -jar <jarfile> encode <namespace>` - encodes a namespace as translation placeholders  
`java -jar <jarfile> decode <encoded namespace>` - decodes translation placeholders back into the original namespac

## Implementation Details
At first, it is determined which character set to use. The program will choose either a reduced character set which only supports [a-z_] or the full character set which will support all of [a-z0-9_.-]. This is done to further compress the data stored in the resulting integers.  
Then, the data gets transformed into what basically is a base 27 number for [a-z_] namespaces, or a base 39 number.  
To ensure the full number can be computed in this step, Java's BigInteger is used here.

Usually, in that base 27 number, the character 'a' is represented as the digit 0.  
However, since the exact length of the number is unkown otherwise, in case the very first letter would be an 'a', this would be an ambigious conversion.  
To avoid this, all values are shifted one over, so 'a' in the first digit will be represented as 1.

To illustrate this, here an example for the case of 3 allowed characters:
```
a = 1
b = 2
c = 3
aa = 4
ab = 5
ac = 6
ba = 7
bb = 8
bc = 9
ca = 10
cb = 11
cc = 12
aaa = 13
aab = 14
aac = 15
aba = 16
abb = 17
abc = 18
aca = 19
⋮
```

This is the algorithm used for the conversion: `curVal := curVal * base + 1 + character_as_digit`. curVal is the previously calculated value, and this calculation is done for every character  
Chracter values:  
a   | b   | c   | d   | e   | f   | g   | h   | i   | j   | k   | l   | m   | n   | o   | p   | q   | r   | s   | t   | u   | v   | w   | x   | y   | z   | _   | 0   | 1   | 2   | 3   | 4   | 5   | 6   | 7   | 8   | 9   | -   | .
--- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---
 0  | 1   | 2   | 3   | 4   | 5   | 6   | 7   | 8   | 9   | 10  | 11  | 12  | 13  | 14  | 15  | 16  | 17  | 18  | 19  | 20  | 21  | 22  | 23  | 24  | 25  | 26  | 27  | 28  | 29  | 30  | 31  | 32  | 33  | 34  | 35  | 36  | 37  | 38

After that initial calculation of the BigtInteger, the value has to be split into 30 bit integers.  
Minecraft uses an signed 32 bit integer representation for translation placeholders, but only accepts positive integers. Thus, usable are only up to 31 bit.  
However, the value 0 is actually also not allowed. While it would be possible to still mostly use all 31 bit, except of the 0 value, I decided to only go for 30 useable bits.  
Bit 31 is always set to 1. This ensures not only a value > 0, but also a value that will be big enough to not interfere with any placeholders that might be needed for different usecases (for example, if this program would generate the placeholder `%1$s` or `%2$s`, the example usecase from above would break due to the namespace prefix)  

Furthermore, the very first usable bit of the first placeholder is used to distinguish the reduced character set [a-z_] from the full character set [a-z0-9_.-], where 0 is the reduced character set and 1 the full character set.

## Data Representation
To summarize, this is the representation of data in this format.

First translate placeholder:
Name          | length (bits) | Description
------------- | ------------- | -----------
One bit       | 1             | Always 1
Character Set | 1             | 0 = Reduced character set, 1 = full character set
Body #1       | 29            | Data block

Every further translate placeholder:
Name          | length (bits) | Description
------------- | ------------- | -----------
One bit       | 1             | Always 1
Body #n       | 30            | Data block
