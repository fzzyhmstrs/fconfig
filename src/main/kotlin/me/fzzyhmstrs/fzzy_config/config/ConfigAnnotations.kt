package me.fzzyhmstrs.fzzy_config.config

/**
 * (Currently unused) A property or section marked as Lockable can be locked by the client player.
 *
 * This annotation should be used on config properties that are not critical to logical performance. For example a color setting or a mob texture variant that the player wants to customize and doesn't want the server to overwrite.
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.CLASS)
annotation class Lockable()

/**
 * (Currently unused) A property or section marked as ClientModifiable will be selectable in the Config GUI by any player on the client, not just a server OP.
 *
 * This annotation should be reserved for non-functional configs like cosmetics, similarly to [Lockable]
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.CLASS)
annotation class ClientModifiable()

@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.CLASS)
annotation class ClientOperatorOnly(val opLevel: Int = 2)

/**
 * Used to define custom text for the config Readme and GUI
 *
 * A validated field that doesn't have an annotated or custom-defined read me text will have a default readme line that generally defines the contents of the field.
 *
 * @param translationKey String, optional. Used to define a translatable text key for translatable readme description. This is the preferred use for this annotation, for better player accessibility
 * @param description String, optional. Used to define a literal text description (not translatable). Defining translation key is preferable. If translation key is used, this is ignored
 * @param header String[], optional. Used to define a literal text header array. It is preferable to define a header within the [ConfigClass] or [ConfigSection] directly, to enable translatable headers
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.CLASS)
annotation class ReadMeText(val translationKey: String = "",val description: String = "", val header: Array<String> = [])

/**
 * A config property marked as NonSync won't be automagically scraped by the
 */
@Target(AnnotationTarget.PROPERTY)
annotation class NonSync()