//  SuperTuxKart - a fun racing game with go-kart
//  Copyright (C) 2009 Marianne Gagnon
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; either version 3
//  of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

#include "challenges/unlock_manager.hpp"
#include "guiengine/widget.hpp"
#include "states_screens/state_manager.hpp"
#include "states_screens/tracks_screen.hpp"
#include "states_screens/dialogs/track_info_dialog.hpp"
#include "tracks/track.hpp"
#include "tracks/track_manager.hpp"
#include "utils/translation.hpp"

using namespace GUIEngine;

TracksScreen::TracksScreen() : Screen("tracks.stkgui")
{
}


void TracksScreen::eventCallback(Widget* widget, const std::string& name)
{
    // -- track seelction screen
    if (name == "tracks")
    {
        DynamicRibbonWidget* w2 = dynamic_cast<DynamicRibbonWidget*>(widget);
        if(w2 != NULL)
        {
            std::cout << "Clicked on track " << w2->getSelectionIDString(GUI_PLAYER_ID).c_str() << std::endl;
            
            Track* clickedTrack = track_manager->getTrack(w2->getSelectionIDString(GUI_PLAYER_ID));
            if (clickedTrack != NULL)
            {
                ITexture* screenshot = GUIEngine::getDriver()->getTexture( clickedTrack->getScreenshotFile().c_str() );
                
                new TrackInfoDialog( clickedTrack->getIdent(), clickedTrack->getName().c_str(), screenshot, 0.8f, 0.7f);
            }
        }
    }
    else if (name == "gps")
    {
        RibbonWidget* w = dynamic_cast<RibbonWidget*>(widget);
        if(w != NULL)
            std::cout << "Clicked on GrandPrix " << w->getSelectionIDString(GUI_PLAYER_ID).c_str() << std::endl;
    }
    
}

void TracksScreen::init()
{
    DynamicRibbonWidget* w = this->getWidget<DynamicRibbonWidget>("tracks");
    assert( w != NULL );
    
    if (!this->m_inited)
    {
        const int trackAmount = track_manager->getNumberOfTracks();
        bool hasLockedTracks = false;
        for (int n=0; n<trackAmount; n++)
        {
            Track* curr = track_manager->getTrack(n);
            if (unlock_manager->isLocked(curr->getIdent()))
            {
                hasLockedTracks = true;
                continue;
            }
            w->addItem(curr->getName(), curr->getIdent(), curr->getScreenshotFile());
        }
        
        if (hasLockedTracks)
        {
            w->addItem(_("Locked Tracks"), "Lock", "textures/gui_lock.png");
        }
        
        this->m_inited = true;
    }
    w->updateItemDisplay();    
}

void TracksScreen::tearDown()
{
}
