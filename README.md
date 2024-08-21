
# **Boulanger**
*/bulɑ̃ʒe/*

noun

##### a person who makes bread and cakes, especially as a trade.


Boulanger completely reinvents bread in Minecraft 1.21. Realistic bakery and milling science as well as a completely reimagined bread crafting system breath new life into the otherwise unrealistic and unimaginative vanilla bread production. This mod is aimed at anyone with an interest in baking and yearns to apply those same concepts in the world of Minecraft. 

Here is a brief overview of the milling process:

>Wheat berries are primarily comprised of three parts; *the germ, bran, and endosperm.* The **endosperm** contains the starch and gluten-forming proteins. To begin milling wheat berries, the berries must be washed, cleaned, and tempered. Tempering involves hydrating the wheat berries under controlled and sanitary conditions. This process facilitates the removal of the bran and germ. Various wheat varieties may be blended prior to milling to achieve a specific quality and gluten content. Additionally this may be done in the shop on an ad hoc basis.
>Wheat is sent through corrugated rollers which crack the kernels. Flour yielded from this stage in the milling process is called "**break flour**". "**Middlings flour**" is the result of the removal of the bran and germ from the flour. Side products are set aside, not discarded. Middlings flour is further sent through smooth rollers to flake out additional germ and bran particles. The remaining flour, deemed "**shorts**" is used for special foods and anmial feed. Further purification results in "**straight flour**", or 100 percent extraction flour. "**Patent**" flour, the highest grade, is removed. Patent flour can be 25-75 percent extraction depending on the flour. Short extraction flour contains the finest particles of flour since it is generally from the center of the endosperm. Longer extraction flours may be darker in color and contain more germ and bran as a result of their proximity to the exterior of the kernel. Long extraction patent flour may be 65-90 percent extraction. A short extraction patent flour may be 25-40 percent extraction. 
>"**Clear flour**" remains after removal of the patent flour. Generally, there are several grades of clear. They are extracted by way of grinding and sifting. 

<sup>[^1](Sultan, 1990, pp. 18-20)</sup>

###### The following section is a mixture of unimplemented and partially implemented features. This is to illustruate my idea in the interum while I write the first usable build so please keep that in mind. To this point, I am not a comp-sci wiz so development pace may be irregular. Every commit should at least build in Idea so if you're a developer you can take a look. 

 
Wild wheat spawns in plains and meadows biomems and will drop a random wheat seed type. There are 5 varieties of wheat: Hard Red Winter, Hard Red Spring, Soft Red Winter, Hard White Spring and Durum. There is soybean, rapeseed(canola), flax and maybe corn and or oil palm. Pine trees spawn in Badlands and Wooded Badlands biome on terracotta blocks. They have their own wood derivative blocks. They can be stripped to collect oleoresin turpentine which can be converted by way of steam distillation to turpentine and rosin. Linseed(flax) oil and turpentine can be used to make oil paints, varnishes and stains. Perhaps linoleum as well?


Wheat production is altered; wheat is grown as normal and then harvested to yield wheat items of that type. Wheat items stack to 91, the rationale will become clear below. 
> - 1 Winchester bushel is 60 lbs / 27.215 kg
> - 1 acre is 4840 yd<sup>2</sup> / 4,425.696 m<sup>2</sup>
> - 48.6 bushels per acre (this is sorta made up)
> - 0.010981 bushels per m<sup>2</sup>
> - 1 wheat item is 4.989g
> - 1 wheat berries item is *n* / 4.989g
> - 1 flour item ??


Pine trees and associated blocks and items. Pine trees can be tapped to collect oleoresin turpentine. With steam distillation, oleoresin can be turned into turpentine and rosin. Turpentine and linseed oil can be used to make oil paints, varnishes and stains. 


WoodGasifierTileEntity will turn wood into ash and wood gas. Wood gas is piped into an internal combustion engine or combustion dynamo. 

Flour can be milled by hand without electricity with the stone mill. When using the stone mill, the only product is whole wheat flour. For all other varieties of flour, you must use the milling equipment. As mentioned in the above paraphrase, milling involves many specialized machines.

- Separator
- Aspirator
- Destoner
- Disc-Separator
- Scourer
- Impact Scourer
- Grinder
- Sifter
- Purifier

additionally, to transport items in-between these machines there will be vacuum flour ducts. Needs a vacuum pump on each intra-net.





**Cited:**

[^1]: **Sultan, W. J. (1990). Practical Baking Fifth Edition. Van Nostrand Reinhold Company.**
