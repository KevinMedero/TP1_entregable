# Associate Navigation Buttons with XML Layouts

The goal is to create three XML layout files in `res/layout` (one for Home, Favorites, and Profile) and display them in the existing `MainActivity` (which uses Jetpack Compose) when the corresponding navigation buttons are clicked.

## Proposed Changes

### Android Resources

#### [NEW] [layout_home.xml](file:///C:/Users/meder/AplicacionesMovilesProjects/TP1_entregable/app/src/main/res/layout/layout_home.xml)
Create a basic layout for the Home screen.

#### [NEW] [layout_favorites.xml](file:///C:/Users/meder/AplicacionesMovilesProjects/TP1_entregable/app/src/main/res/layout/layout_favorites.xml)
Create a basic layout for the Favorites screen.

#### [NEW] [layout_profile.xml](file:///C:/Users/meder/AplicacionesMovilesProjects/TP1_entregable/app/src/main/res/layout/layout_profile.xml)
Create a basic layout for the Profile screen.

### App Component

#### [MODIFY] [MainActivity.kt](file:///C:/Users/meder/AplicacionesMovilesProjects/TP1_entregable/app/src/main/java/com/example/tp1_entregable/MainActivity.kt)
Update the `TP1_entregableApp` composable to use `AndroidView` to inflate and display the corresponding XML layout based on the `currentDestination` state.

## Verification Plan

### Manual Verification
1.  Run the application.
2.  Click on the "Home" icon in the navigation bar and verify that the content from `layout_home.xml` is displayed.
3.  Click on the "Favorites" icon and verify that `layout_favorites.xml` is displayed.
4.  Click on the "Profile" icon and verify that `layout_profile.xml` is displayed.
